package com.filemanager.android.features.files;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.filemanager.android.R;
import com.filemanager.android.network.ApiClient;
import com.filemanager.android.network.ApiService;
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

    // ===== Dependencies =====
    private ApiService apiService;
    private FileAdapter fileAdapter;

    // ===== File Picker Launcher =====
    private ActivityResultLauncher<Intent> filePickerLauncher;

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
        setupRecyclerView();
        setupSwipeRefresh();
        setupFab();
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

    /** Đăng ký file picker (ACTION_GET_CONTENT) */
    private void setupFilePicker() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK
                            && result.getData() != null
                            && result.getData().getData() != null) {
                        Uri selectedUri = result.getData().getData();
                        uploadFile(selectedUri);
                    }
                }
        );
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
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(intent);
    }

    /**
     * Upload file được chọn từ file picker lên server.
     * Tạo MultipartBody từ Uri và gọi POST /api/files/upload
     */
    private void uploadFile(Uri fileUri) {
        String fileName = FileUtils.getFileName(requireContext(), fileUri);
        String mimeType = FileUtils.getMimeType(requireContext(), fileUri);

        showToast("⬆️ Đang tải lên: " + fileName + "...");

        try {
            // Đọc file thành bytes
            byte[] fileBytes = readAllBytes(fileUri);

            RequestBody fileBody = RequestBody.create(
                    MediaType.parse(mimeType), fileBytes);

            MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                    "file", fileName, fileBody);

            // parentId (null nếu đang ở root)
            RequestBody parentIdBody = currentParentId != null
                    ? RequestBody.create(MediaType.parse("text/plain"),
                      String.valueOf(currentParentId))
                    : RequestBody.create(MediaType.parse("text/plain"), "");

            apiService.uploadFile(filePart, parentIdBody)
                    .enqueue(new Callback<FileMetadataDto>() {
                @Override
                public void onResponse(Call<FileMetadataDto> call,
                                       Response<FileMetadataDto> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        showToast("✅ Tải lên thành công: " + fileName);
                        fileAdapter.addItem(response.body());
                        toggleEmptyState(false);
                    } else if (response.code() == 413) {
                        showToast("⚠️ File quá lớn (giới hạn 10MB)");
                    } else {
                        showToast("❌ Tải lên thất bại");
                    }
                }

                @Override
                public void onFailure(Call<FileMetadataDto> call, Throwable t) {
                    showToast(getString(R.string.err_network));
                }
            });

        } catch (IOException e) {
            showToast("Không thể đọc file: " + e.getMessage());
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
                    showToast("❌ Không thể tạo thư mục");
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

        // Rename (TODO Phase 2 nâng cao)
        sheetView.findViewById(R.id.action_rename).setOnClickListener(v -> {
            dialog.dismiss();
            showToast("Tính năng đổi tên sẽ được bổ sung sớm");
        });

        // Delete
        sheetView.findViewById(R.id.action_delete).setOnClickListener(v -> {
            dialog.dismiss();
            confirmDelete(file);
        });

        dialog.show();
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
