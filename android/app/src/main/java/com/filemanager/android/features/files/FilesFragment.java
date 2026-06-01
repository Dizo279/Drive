package com.filemanager.android.features.files;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.filemanager.android.R;
import com.filemanager.android.network.ApiClient;
import com.filemanager.android.network.ApiService;
import com.filemanager.android.network.ProgressRequestBody;
import com.filemanager.android.network.dto.FileMetadataDto;
import com.filemanager.android.utils.FileUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragment chính quản lý file/folder.
 *
 * Tính năng:
 * - Xem danh sách file/folder theo thư mục
 * - Breadcrumb navigation (duyệt thư mục con)
 * - Upload file từ bộ nhớ thiết bị
 * - Tạo thư mục mới
 * - Xóa file/folder (soft delete → Trash)
 * - Download file
 * - FAB menu (Upload / New Folder)
 * - Bottom Sheet context menu
 * - Pull-to-refresh
 */
public class FilesFragment extends Fragment implements FileAdapter.OnFileActionListener {

    // ===== State =====
    /** Stack lưu lịch sử điều hướng: (parentId, folderName) */
    private final Stack<BreadcrumbItem> breadcrumbStack = new Stack<>();
    private Long currentParentId = null; // null = root

    // ===== Views =====
    private RecyclerView recyclerFiles;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutEmpty;
    private LinearLayout breadcrumbContainer;
    private FloatingActionButton fabAdd;
    private EditText etSearch;
    private ImageButton btnSort;
    private View layoutUploadProgress;
    private TextView tvUploadProgressText;
    private ProgressBar pbUpload;

    // ===== Dependencies =====
    private ApiService apiService;
    private FileAdapter fileAdapter;

    // ===== File Picker Launcher =====
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ActivityResultLauncher<String[]> permissionLauncher;

    // ==================================================================
    // Lifecycle
    // ==================================================================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_files, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        apiService = ApiClient.getApiService(requireContext());

        setupFilePicker();
        setupPermissionLauncher();
        setupRecyclerView();
        setupSwipeRefresh();
        setupFab();
        setupSearchAndSort();
        loadFiles();
    }

    // ==================================================================
    // Setup methods
    // ==================================================================

    private void initViews(View view) {
        recyclerFiles      = view.findViewById(R.id.recycler_files);
        swipeRefresh       = view.findViewById(R.id.swipe_refresh);
        layoutEmpty        = view.findViewById(R.id.layout_empty);
        breadcrumbContainer = view.findViewById(R.id.breadcrumb_container);
        fabAdd             = view.findViewById(R.id.fab_add);
        etSearch           = view.findViewById(R.id.et_search);
        btnSort            = view.findViewById(R.id.btn_sort);
        layoutUploadProgress = view.findViewById(R.id.layout_upload_progress);
        tvUploadProgressText = view.findViewById(R.id.tv_upload_progress_text);
        pbUpload           = view.findViewById(R.id.pb_upload);
    }

    private void setupRecyclerView() {
        fileAdapter = new FileAdapter(requireContext(), this);
        recyclerFiles.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerFiles.setAdapter(fileAdapter);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(
                requireContext().getColor(R.color.apple_blue)
        );
        swipeRefresh.setOnRefreshListener(this::loadFiles);
    }

    /** FAB hiển thị popup menu với 2 lựa chọn: Upload File / Tạo thư mục */
    private void setupFab() {
        fabAdd.setOnClickListener(v -> showFabMenu());
    }

    private void setupPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean granted = true;
                    for (Boolean ok : result.values()) {
                        if (!Boolean.TRUE.equals(ok)) {
                            granted = false;
                            break;
                        }
                    }
                    if (granted) {
                        launchFilePickerIntent();
                    } else {
                        showToast("Cần quyền đọc file để tải lên");
                    }
                }
        );
    }

    /** Đăng ký file picker (ACTION_GET_CONTENT) */
    private void setupFilePicker() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        if (data.getClipData() != null) {
                            ClipData clipData = data.getClipData();
                            List<Uri> uris = new ArrayList<>();
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                uris.add(clipData.getItemAt(i).getUri());
                            }
                            uploadMultipleFiles(uris);
                        } else if (data.getData() != null) {
                            List<Uri> uris = new ArrayList<>();
                            uris.add(data.getData());
                            uploadMultipleFiles(uris);
                        }
                    }
                }
        );
    }

    private void setupSearchAndSort() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                fileAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSort.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), v);
            popup.getMenu().add(0, 0, 0, "Tên (A-Z)");
            popup.getMenu().add(0, 1, 1, "Tên (Z-A)");
            popup.getMenu().add(0, 2, 2, "Mới nhất");
            popup.getMenu().add(0, 3, 3, "Kích thước (Lớn đến bé)");
            popup.setOnMenuItemClickListener(item -> {
                fileAdapter.sort(item.getItemId());
                return true;
            });
            popup.show();
        });
    }

    // ==================================================================
    // Load files
    // ==================================================================

    private void loadFiles() {
        swipeRefresh.setRefreshing(true);

        apiService.getFiles(currentParentId).enqueue(new Callback<List<FileMetadataDto>>() {
            @Override
            public void onResponse(Call<List<FileMetadataDto>> call,
                                   Response<List<FileMetadataDto>> response) {
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<FileMetadataDto> files = response.body();
                    fileAdapter.setData(files);
                    toggleEmptyState(files.isEmpty());
                } else {
                    showToast("Không thể tải danh sách file");
                    toggleEmptyState(true);
                }
            }

            @Override
            public void onFailure(Call<List<FileMetadataDto>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                showToast(getString(R.string.err_network));
                toggleEmptyState(true);
            }
        });
    }

    // ==================================================================
    // FileAdapter.OnFileActionListener callbacks
    // ==================================================================

    @Override
    public void onFileClick(FileMetadataDto file) {
        if (file.isFolder()) {
            // Vào thư mục con
            breadcrumbStack.push(new BreadcrumbItem(currentParentId, getCurrentFolderName()));
            currentParentId = file.getId();
            updateBreadcrumb(file.getFileName());
            loadFiles();
        } else {
            // Với file thường → hiện bottom sheet options
            showFileOptionsSheet(file);
        }
    }

    @Override
    public void onMoreOptionsClick(FileMetadataDto file) {
        showFileOptionsSheet(file);
    }

    // ==================================================================
    // Breadcrumb navigation
    // ==================================================================

    /**
     * Cập nhật thanh breadcrumb khi điều hướng vào thư mục mới.
     */
    private void updateBreadcrumb(String folderName) {
        if (breadcrumbContainer == null) return;
        breadcrumbContainer.removeAllViews();

        // Home
        TextView tvHome = makeBreadcrumbItem("🏠 Home", true);
        tvHome.setOnClickListener(v -> navigateToRoot());
        breadcrumbContainer.addView(tvHome);

        // Các cấp thư mục từ stack
        for (BreadcrumbItem item : breadcrumbStack) {
            if (item.parentId != null) {
                addBreadcrumbSeparator();
                final BreadcrumbItem breadcrumb = item;
                TextView tvItem = makeBreadcrumbItem(item.name, true);
                tvItem.setOnClickListener(v -> navigateToBreadcrumb(breadcrumb));
                breadcrumbContainer.addView(tvItem);
            }
        }

        // Thư mục hiện tại (không clickable)
        addBreadcrumbSeparator();
        breadcrumbContainer.addView(makeBreadcrumbItem(folderName, false));
    }

    private TextView makeBreadcrumbItem(String text, boolean isClickable) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(13f);
        tv.setTextColor(isClickable
                ? requireContext().getColor(R.color.apple_blue)
                : requireContext().getColor(R.color.apple_black));
        // Normal weight for clickable breadcrumb items
        return tv;
    }

    private void addBreadcrumbSeparator() {
        TextView sep = new TextView(requireContext());
        sep.setText("  ›  ");
        sep.setTextSize(13f);
        sep.setTextColor(requireContext().getColor(R.color.apple_gray));
        breadcrumbContainer.addView(sep);
    }

    private void navigateToRoot() {
        breadcrumbStack.clear();
        currentParentId = null;
        if (breadcrumbContainer != null) {
            breadcrumbContainer.removeAllViews();
            TextView tvHome = makeBreadcrumbItem("🏠 Home", false);
            breadcrumbContainer.addView(tvHome);
        }
        loadFiles();
    }

    private void navigateToBreadcrumb(BreadcrumbItem target) {
        // Pop stack đến item được chọn
        while (!breadcrumbStack.isEmpty()
                && !breadcrumbStack.peek().equals(target)) {
            breadcrumbStack.pop();
        }
        if (!breadcrumbStack.isEmpty()) {
            BreadcrumbItem item = breadcrumbStack.pop();
            currentParentId = item.parentId;
            loadFiles();
        }
    }

    private String getCurrentFolderName() {
        return breadcrumbStack.isEmpty() ? "Home" : breadcrumbStack.peek().name;
    }

    /**
     * Gọi từ Activity khi người dùng nhấn nút Back của Android.
     * @return true nếu fragment tự xử lý (đang ở thư mục con), false nếu Activity tự xử lý
     */
    public boolean onBackPressed() {
        if (!breadcrumbStack.isEmpty()) {
            BreadcrumbItem parent = breadcrumbStack.pop();
            currentParentId = parent.parentId;
            updateBreadcrumb(parent.name);
            loadFiles();
            return true;
        }
        return false;
    }

    // ==================================================================
    // FAB Menu
    // ==================================================================

    private void showFabMenu() {
        // AlertDialog đơn giản với 2 lựa chọn
        new AlertDialog.Builder(requireContext())
                .setTitle("Thêm mới")
                .setItems(new String[]{"⬆️  Tải file lên", "📁  Tạo thư mục mới"}, (dialog, which) -> {
                    if (which == 0) openFilePicker();
                    else showCreateFolderDialog();
                })
                .show();
    }

    // ==================================================================
    // Upload File
    // ==================================================================

    private void openFilePicker() {
        if (!hasStoragePermission()) {
            permissionLauncher.launch(getRequiredStoragePermissions());
            return;
        }
        launchFilePickerIntent();
    }

    private void launchFilePickerIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        filePickerLauncher.launch(intent);
    }

    private boolean hasStoragePermission() {
        for (String perm : getRequiredStoragePermissions()) {
            if (ContextCompat.checkSelfPermission(requireContext(), perm)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private String[] getRequiredStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_VIDEO,
                    android.Manifest.permission.READ_MEDIA_AUDIO
            };
        }
        return new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE};
    }

    private void uploadMultipleFiles(List<Uri> uris) {
        if (uris == null || uris.isEmpty()) return;
        layoutUploadProgress.setVisibility(View.VISIBLE);
        pbUpload.setProgress(0);
        uploadNextFile(uris, 0, uris.size());
    }

    private void uploadNextFile(List<Uri> uris, int index, int total) {
        if (index >= total) {
            layoutUploadProgress.setVisibility(View.GONE);
            showToast("✅ Đã tải lên xong " + total + " file");
            loadFiles();
            return;
        }

        Uri fileUri = uris.get(index);
        String rawName = FileUtils.getFileName(requireContext(), fileUri);
        final String fileName = (rawName == null || rawName.isEmpty()) ? "upload.bin" : rawName;
        String rawMime = FileUtils.getMimeType(requireContext(), fileUri);
        final String mimeType = (rawMime == null || rawMime.isEmpty())
                ? "application/octet-stream" : rawMime;

        tvUploadProgressText.setText("Đang tải lên " + (index + 1) + "/" + total + ": " + fileName);
        pbUpload.setProgress(0);

        try {
            byte[] fileBytes = readAllBytes(fileUri);
            if (fileBytes.length == 0) {
                showToast("File rỗng: " + fileName);
                uploadNextFile(uris, index + 1, total);
                return;
            }

            MediaType mediaType = MediaType.parse(mimeType);
            RequestBody fileBody = RequestBody.create(fileBytes, mediaType);

            ProgressRequestBody progressRequestBody = new ProgressRequestBody(fileBody, percentage -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> pbUpload.setProgress(percentage));
                }
            });

            MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                    "file", fileName, progressRequestBody);

            apiService.uploadFile(filePart, currentParentId)
                    .enqueue(new Callback<FileMetadataDto>() {
                @Override
                public void onResponse(Call<FileMetadataDto> call, Response<FileMetadataDto> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        fileAdapter.addItem(response.body());
                        toggleEmptyState(false);
                    } else if (response.code() == 413) {
                        showToast("⚠️ File " + fileName + " quá lớn (tối đa 10MB)");
                    } else {
                        String err = readErrorBody(response);
                        showToast("❌ Lỗi tải lên " + fileName + ": " + err);
                    }
                    // Tiếp tục file tiếp theo
                    uploadNextFile(uris, index + 1, total);
                }

                @Override
                public void onFailure(Call<FileMetadataDto> call, Throwable t) {
                    showToast(getString(R.string.err_network) + " (" + fileName + ")");
                    uploadNextFile(uris, index + 1, total);
                }
            });

        } catch (IOException e) {
            showToast("Không thể đọc file: " + fileName);
            uploadNextFile(uris, index + 1, total);
        }
    }

    /** Đọc toàn bộ bytes từ Uri (phù hợp với file ≤ 10MB theo giới hạn backend) */
    private byte[] readAllBytes(Uri uri) throws IOException {
        try (java.io.InputStream is = requireContext()
                .getContentResolver().openInputStream(uri)) {
            if (is == null) throw new IOException("Cannot open stream");
            return is.readAllBytes();
        }
    }

    // ==================================================================
    // Create Folder
    // ==================================================================

    private void showCreateFolderDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_create_folder, null);

        TextInputLayout til = dialogView.findViewById(R.id.til_folder_name);
        TextInputEditText et = dialogView.findViewById(R.id.et_folder_name);

        new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Tạo", (dialog, which) -> {
                    String name = et.getText() != null
                            ? et.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(name)) {
                        til.setError("Tên thư mục không được để trống");
                        return;
                    }
                    createFolder(name);
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void createFolder(String folderName) {
        Map<String, Object> body = new HashMap<>();
        body.put("fileName", folderName);
        if (currentParentId != null) {
            body.put("parentId", currentParentId);
        }

        apiService.createFolder(body).enqueue(new Callback<FileMetadataDto>() {
            @Override
            public void onResponse(Call<FileMetadataDto> call,
                                   Response<FileMetadataDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showToast("📁 Đã tạo thư mục: " + folderName);
                    fileAdapter.addItem(response.body());
                    toggleEmptyState(false);
                } else {
                    showToast("❌ Không thể tạo thư mục (HTTP " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<FileMetadataDto> call, Throwable t) {
                showToast(getString(R.string.err_network));
            }
        });
    }

    // ==================================================================
    // File Options Bottom Sheet
    // ==================================================================

    private void showFileOptionsSheet(FileMetadataDto file) {
        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_file_options, null);

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sheetView);

        // Tiêu đề file
        TextView tvName = sheetView.findViewById(R.id.tv_bs_file_name);
        tvName.setText(file.getFileName());

        // Ẩn Download nếu là folder
        View actionDownload = sheetView.findViewById(R.id.action_download);
        if (file.isFolder()) {
            actionDownload.setVisibility(View.GONE);
        } else {
            actionDownload.setOnClickListener(v -> {
                dialog.dismiss();
                downloadFile(file);
            });
        }

        // Share → gọi SharedFragment.showShareSheet()
        sheetView.findViewById(R.id.action_share).setOnClickListener(v -> {
            dialog.dismiss();
            com.filemanager.android.features.shared.SharedFragment.showShareSheet(
                    requireContext(), file.getId(), file.getFileName(), apiService);
        });

        // Rename
        sheetView.findViewById(R.id.action_rename).setOnClickListener(v -> {
            dialog.dismiss();
            showRenameDialog(file);
        });

        // Delete
        sheetView.findViewById(R.id.action_delete).setOnClickListener(v -> {
            dialog.dismiss();
            confirmDelete(file);
        });

        dialog.show();
    }

    // ==================================================================
    // Rename File/Folder
    // ==================================================================

    private void showRenameDialog(FileMetadataDto file) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_create_folder, null);

        TextInputLayout til = dialogView.findViewById(R.id.til_folder_name);
        TextInputEditText et = dialogView.findViewById(R.id.et_folder_name);
        
        til.setHint("Tên mới");
        et.setText(file.getFileName());

        new AlertDialog.Builder(requireContext())
                .setTitle("Đổi tên")
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newName = et.getText() != null
                            ? et.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(newName)) {
                        til.setError("Tên không được để trống");
                        return;
                    }
                    if (newName.equals(file.getFileName())) return;
                    
                    renameFile(file, newName);
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void renameFile(FileMetadataDto file, String newName) {
        Map<String, String> body = new HashMap<>();
        body.put("name", newName);

        apiService.renameFile(file.getId(), body).enqueue(new Callback<FileMetadataDto>() {
            @Override
            public void onResponse(Call<FileMetadataDto> call, Response<FileMetadataDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showToast("✅ Đã đổi tên thành: " + newName);
                    loadFiles(); // Reload to keep proper sorting
                } else {
                    showToast("❌ Không thể đổi tên");
                }
            }

            @Override
            public void onFailure(Call<FileMetadataDto> call, Throwable t) {
                showToast(getString(R.string.err_network));
            }
        });
    }

    // ==================================================================
    // Download File
    // ==================================================================

    private void downloadFile(FileMetadataDto file) {
        showToast("⬇️ Đang tải xuống: " + file.getFileName() + "...");

        apiService.downloadFile(file.getId()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        File savedFile = FileUtils.saveToDownloads(
                                requireContext(), response.body(), file.getFileName());
                        showToast("✅ Đã tải: " + savedFile.getAbsolutePath());
                    } catch (IOException e) {
                        showToast("❌ Lỗi lưu file: " + e.getMessage());
                    }
                } else {
                    showToast("❌ Tải xuống thất bại");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showToast(getString(R.string.err_network));
            }
        });
    }

    // ==================================================================
    // Delete File
    // ==================================================================

    private void confirmDelete(FileMetadataDto file) {
        String message = file.isFolder()
                ? "Chuyển thư mục \"" + file.getFileName() + "\" vào thùng rác?"
                : "Chuyển \"" + file.getFileName() + "\" vào thùng rác?";

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.confirm_delete))
                .setMessage(message)
                .setPositiveButton("Xóa", (d, w) -> deleteFile(file))
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void deleteFile(FileMetadataDto file) {
        apiService.deleteFile(file.getId()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    fileAdapter.removeItem(file);
                    showToast("🗑️ Đã chuyển vào thùng rác");
                    toggleEmptyState(fileAdapter.getItemCount() == 0);
                } else {
                    showToast("❌ Không thể xóa file");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showToast(getString(R.string.err_network));
            }
        });
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private void toggleEmptyState(boolean isEmpty) {
        recyclerFiles.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private String readErrorBody(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String body = response.errorBody().string();
                if (body.contains("\"error\"")) {
                    int start = body.indexOf("\"error\"");
                    int colon = body.indexOf(':', start);
                    int q1 = body.indexOf('"', colon + 1);
                    int q2 = body.indexOf('"', q1 + 1);
                    if (q2 > q1) {
                        return body.substring(q1 + 1, q2);
                    }
                }
                if (body.length() > 120) {
                    return body.substring(0, 120);
                }
                return body;
            }
        } catch (Exception ignored) {
        }
        return "HTTP " + response.code();
    }

    // ==================================================================
    // Inner class: Breadcrumb item
    // ==================================================================

    /**
     * Đại diện cho một cấp thư mục trong lịch sử điều hướng.
     */
    private static class BreadcrumbItem {
        final Long parentId;
        final String name;

        BreadcrumbItem(Long parentId, String name) {
            this.parentId = parentId;
            this.name = name != null ? name : "Home";
        }
    }
}
