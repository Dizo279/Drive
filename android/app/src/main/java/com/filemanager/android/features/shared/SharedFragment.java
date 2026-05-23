package com.filemanager.android.features.shared;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.filemanager.android.network.dto.SharedItemDto;
import com.filemanager.android.utils.FileUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragment Chia sẻ file.
 *
 * 2 tab toggle:
 *  - "Tôi đã chia sẻ": Danh sách file tôi chia sẻ + nút Thu hồi
 *  - "Chia sẻ với tôi": Danh sách file người khác chia sẻ + nút Tải xuống
 *
 * Tính năng:
 * - Load 2 danh sách riêng từ API
 * - Revoke share (thu hồi quyền)
 * - Download file được chia sẻ qua shareToken
 * - Pull-to-refresh
 */
public class SharedFragment extends Fragment implements SharedItemAdapter.OnSharedActionListener {

    private MaterialButtonToggleGroup toggleShared;
    private MaterialButton btnByMe, btnWithMe;
    private RecyclerView recyclerShared;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutEmpty;

    private ApiService apiService;
    private SharedItemAdapter sharedAdapter;

    private boolean isShowingByMe = true; // Mặc định hiển thị "Tôi đã chia sẻ"

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shared, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        apiService = ApiClient.getApiService(requireContext());

        setupAdapter();
        setupToggle();
        setupSwipeRefresh();

        // Chọn tab đầu tiên
        toggleShared.check(R.id.btn_shared_by_me);
        loadSharedByMe();
    }

    private void initViews(View view) {
        toggleShared   = view.findViewById(R.id.toggle_shared);
        btnByMe        = view.findViewById(R.id.btn_shared_by_me);
        btnWithMe      = view.findViewById(R.id.btn_shared_with_me);
        recyclerShared = view.findViewById(R.id.recycler_shared);
        swipeRefresh   = view.findViewById(R.id.swipe_refresh_shared);
        layoutEmpty    = view.findViewById(R.id.layout_empty_shared);
    }

    private void setupAdapter() {
        sharedAdapter = new SharedItemAdapter(requireContext(), true, this);
        recyclerShared.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerShared.setAdapter(sharedAdapter);
    }

    private void setupToggle() {
        toggleShared.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btn_shared_by_me) {
                isShowingByMe = true;
                loadSharedByMe();
            } else {
                isShowingByMe = false;
                loadSharedWithMe();
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(requireContext().getColor(R.color.apple_blue));
        swipeRefresh.setOnRefreshListener(() -> {
            if (isShowingByMe) loadSharedByMe();
            else loadSharedWithMe();
        });
    }

    // ==================================================================
    // Load Shared Lists
    // ==================================================================

    private void loadSharedByMe() {
        swipeRefresh.setRefreshing(true);
        apiService.getFilesSharedByMe().enqueue(new Callback<List<SharedItemDto>>() {
            @Override
            public void onResponse(Call<List<SharedItemDto>> call,
                                   Response<List<SharedItemDto>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<SharedItemDto> items = response.body();
                    sharedAdapter.setData(items, true);
                    toggleEmptyState(items.isEmpty());
                } else {
                    toggleEmptyState(true);
                }
            }

            @Override
            public void onFailure(Call<List<SharedItemDto>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                showToast(getString(R.string.err_network));
                toggleEmptyState(true);
            }
        });
    }

    private void loadSharedWithMe() {
        swipeRefresh.setRefreshing(true);
        apiService.getFilesSharedWithMe().enqueue(new Callback<List<SharedItemDto>>() {
            @Override
            public void onResponse(Call<List<SharedItemDto>> call,
                                   Response<List<SharedItemDto>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<SharedItemDto> items = response.body();
                    sharedAdapter.setData(items, false);
                    toggleEmptyState(items.isEmpty());
                } else {
                    toggleEmptyState(true);
                }
            }

            @Override
            public void onFailure(Call<List<SharedItemDto>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                showToast(getString(R.string.err_network));
                toggleEmptyState(true);
            }
        });
    }

    // ==================================================================
    // SharedItemAdapter.OnSharedActionListener callbacks
    // ==================================================================

    @Override
    public void onRevoke(SharedItemDto item) {
        confirmRevoke(item);
    }

    @Override
    public void onDownload(SharedItemDto item) {
        if (item.getShareToken() == null) {
            showToast("Không có link tải xuống");
            return;
        }
        downloadSharedFile(item);
    }

    // ==================================================================
    // Revoke Share
    // ==================================================================

    private void confirmRevoke(SharedItemDto item) {
        String msg = "Thu hồi quyền truy cập của "
                + (item.getTargetEmail() != null ? item.getTargetEmail() : "người dùng này")
                + " vào file \"" + item.getFileName() + "\"?";

        new AlertDialog.Builder(requireContext())
                .setTitle("Thu hồi chia sẻ")
                .setMessage(msg)
                .setPositiveButton("Thu hồi", (d, w) -> revokeShare(item))
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void revokeShare(SharedItemDto item) {
        apiService.revokeShare(item.getShareId()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    sharedAdapter.removeItem(item);
                    showToast("✅ Đã thu hồi quyền chia sẻ");
                    toggleEmptyState(sharedAdapter.getItemCount() == 0);
                } else {
                    showToast("❌ Không thể thu hồi");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showToast(getString(R.string.err_network));
            }
        });
    }

    // ==================================================================
    // Download Shared File (via token)
    // ==================================================================

    private void downloadSharedFile(SharedItemDto item) {
        showToast("⬇️ Đang tải: " + item.getFileName() + "...");

        apiService.downloadSharedFile(item.getShareToken())
                .enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String fileName = item.getFileName() != null
                                ? item.getFileName() : "shared_file";
                        File savedFile = FileUtils.saveToDownloads(
                                requireContext(), response.body(), fileName);
                        showToast("✅ Đã tải: " + savedFile.getName());
                    } catch (IOException e) {
                        showToast("❌ Lỗi lưu file: " + e.getMessage());
                    }
                } else if (response.code() == 410) {
                    showToast("⚠️ Link đã hết hạn hoặc bị thu hồi");
                } else {
                    showToast("❌ Tải thất bại");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showToast(getString(R.string.err_network));
            }
        });
    }

    // ==================================================================
    // Public Share Sheet (được gọi từ FilesFragment)
    // ==================================================================

    /**
     * Hiển thị Bottom Sheet chia sẻ file.
     * Gọi từ FilesFragment.showFileOptionsSheet() khi người dùng chọn "Chia sẻ".
     *
     * @param context  Context để inflate view
     * @param fileId   ID của file cần chia sẻ
     * @param fileName Tên file để hiển thị
     * @param apiSvc   ApiService instance từ fragment gọi
     */
    public static void showShareSheet(Context context, Long fileId, String fileName,
                                      ApiService apiSvc) {
        View sheetView = LayoutInflater.from(context)
                .inflate(R.layout.bottom_sheet_share, null);

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        dialog.setContentView(sheetView);

        // Tên file
        TextView tvShareFileName = sheetView.findViewById(R.id.tv_share_file_name);
        tvShareFileName.setText(fileName);

        TextInputLayout tilEmail = sheetView.findViewById(R.id.til_share_email);
        TextInputEditText etEmail = sheetView.findViewById(R.id.et_share_email);
        MaterialButtonToggleGroup toggleExpiry = sheetView.findViewById(R.id.toggle_expiry);
        MaterialButton btnShareEmail = sheetView.findViewById(R.id.btn_share_email);
        MaterialButton btnCreatePublicLink = sheetView.findViewById(R.id.btn_create_public_link);
        LinearLayout layoutResult = sheetView.findViewById(R.id.layout_share_result);
        TextView tvResultLink = sheetView.findViewById(R.id.tv_share_result_link);
        MaterialButton btnCopyLink = sheetView.findViewById(R.id.btn_copy_link);

        // Chọn mặc định 7 ngày
        toggleExpiry.check(R.id.btn_expire_7);

        /** Helper: lấy số ngày từ toggle */
        int[] getExpireDays = {7};
        toggleExpiry.addOnButtonCheckedListener((g, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btn_expire_7)        getExpireDays[0] = 7;
            else if (checkedId == R.id.btn_expire_30)  getExpireDays[0] = 30;
            else                                        getExpireDays[0] = 0; // never
        });

        // Chia sẻ qua email
        btnShareEmail.setOnClickListener(v -> {
            tilEmail.setError(null);
            String emailInput = etEmail.getText() != null
                    ? etEmail.getText().toString().trim() : "";

            if (TextUtils.isEmpty(emailInput)) {
                tilEmail.setError("Nhập ít nhất 1 email");
                return;
            }

            // Parse danh sách email (cách nhau bởi dấu phẩy)
            List<String> emails = Arrays.asList(emailInput.split("[,;\\s]+"));

            Map<String, Object> body = new HashMap<>();
            body.put("emails", emails);
            if (getExpireDays[0] > 0) {
                body.put("expireDays", getExpireDays[0]);
            }

            btnShareEmail.setEnabled(false);
            btnShareEmail.setText("Đang chia sẻ...");

            apiSvc.shareFile(fileId, body).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    btnShareEmail.setEnabled(true);
                    btnShareEmail.setText("Chia sẻ qua Email");
                    if (response.isSuccessful()) {
                        Toast.makeText(context,
                                "✅ Đã chia sẻ với " + emails.size() + " người",
                                Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(context, "❌ Không thể chia sẻ", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    btnShareEmail.setEnabled(true);
                    btnShareEmail.setText("Chia sẻ qua Email");
                    Toast.makeText(context, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Tạo public link
        btnCreatePublicLink.setOnClickListener(v -> {
            Map<String, Object> body = new HashMap<>();
            // emails rỗng → backend tạo public token
            body.put("emails", new String[]{});
            if (getExpireDays[0] > 0) {
                body.put("expireDays", getExpireDays[0]);
            }

            btnCreatePublicLink.setEnabled(false);
            btnCreatePublicLink.setText("Đang tạo link...");

            apiSvc.shareFile(fileId, body).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    btnCreatePublicLink.setEnabled(true);
                    btnCreatePublicLink.setText("🌐  Tạo Public Link");

                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String token = response.body().string();
                            // Hiển thị link
                            String shareLink = ApiClient.BASE_URL + "files/shared/" + token;
                            tvResultLink.setText(shareLink);
                            layoutResult.setVisibility(View.VISIBLE);

                            // Copy link
                            btnCopyLink.setOnClickListener(cv -> {
                                ClipboardManager clipboard = (ClipboardManager)
                                        context.getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("Share Link", shareLink);
                                clipboard.setPrimaryClip(clip);
                                Toast.makeText(context, "✅ Đã sao chép link",
                                        Toast.LENGTH_SHORT).show();
                            });
                        } catch (IOException e) {
                            tvResultLink.setText("Link đã được tạo");
                            layoutResult.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Toast.makeText(context, "❌ Không thể tạo link", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    btnCreatePublicLink.setEnabled(true);
                    btnCreatePublicLink.setText("🌐  Tạo Public Link");
                    Toast.makeText(context, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private void toggleEmptyState(boolean isEmpty) {
        recyclerShared.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void showToast(String msg) {
        if (getContext() != null)
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
