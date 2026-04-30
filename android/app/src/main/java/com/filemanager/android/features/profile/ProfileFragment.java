package com.filemanager.android.features.profile;

import android.content.Intent;
import android.os.Bundle;
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

import com.filemanager.android.MainActivity;
import com.filemanager.android.R;
import com.filemanager.android.network.ApiClient;
import com.filemanager.android.network.ApiService;
import com.filemanager.android.network.dto.UserDto;
import com.filemanager.android.storage.SessionManager;
import com.filemanager.android.utils.FileUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragment Hồ sơ cá nhân.
 *
 * Tính năng:
 * - Hiển thị avatar (chữ cái đầu tên), fullName, email, role
 * - Storage quota với ProgressBar và số liệu đã dùng / tổng
 * - Nút Nâng cấp Premium (POST /api/users/upgrade-request)
 * - Nút Đăng xuất với xác nhận
 * - Thông tin tài khoản (username, email, role)
 */
public class ProfileFragment extends Fragment {

    // === Header views ===
    private TextView tvAvatarLetter;
    private TextView tvFullName;
    private TextView tvEmail;
    private TextView tvTierBadge;

    // === Quota views ===
    private LinearProgressIndicator progressQuota;
    private TextView tvQuotaPercent;
    private TextView tvQuotaUsed;
    private TextView tvQuotaMax;
    private MaterialButton btnUpgradePremium;

    // === Profile info views ===
    private LinearProgressIndicator progressProfile;
    private LinearLayout layoutProfileInfo;
    private View dividerInfo;
    private LinearLayout layoutEmailInfo;
    private View dividerEmail;
    private LinearLayout layoutRoleInfo;
    private TextView tvInfoUsername;
    private TextView tvInfoEmail;
    private TextView tvInfoRole;

    // === Action views ===
    private LinearLayout actionLogout;

    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        apiService = ApiClient.getApiService(requireContext());

        setupActions();
        loadProfile();
    }

    private void initViews(View view) {
        // Header
        tvAvatarLetter   = view.findViewById(R.id.tv_avatar_letter);
        tvFullName       = view.findViewById(R.id.tv_full_name);
        tvEmail          = view.findViewById(R.id.tv_email);
        tvTierBadge      = view.findViewById(R.id.tv_tier_badge);

        // Quota
        progressQuota    = view.findViewById(R.id.progress_quota);
        tvQuotaPercent   = view.findViewById(R.id.tv_quota_percent);
        tvQuotaUsed      = view.findViewById(R.id.tv_quota_used);
        tvQuotaMax       = view.findViewById(R.id.tv_quota_max);
        btnUpgradePremium = view.findViewById(R.id.btn_upgrade_premium);

        // Profile info card
        progressProfile  = view.findViewById(R.id.progress_profile);
        layoutProfileInfo = view.findViewById(R.id.layout_profile_info);
        dividerInfo       = view.findViewById(R.id.divider_info);
        layoutEmailInfo   = view.findViewById(R.id.layout_email_info);
        dividerEmail      = view.findViewById(R.id.divider_email);
        layoutRoleInfo    = view.findViewById(R.id.layout_role_info);
        tvInfoUsername    = view.findViewById(R.id.tv_info_username);
        tvInfoEmail       = view.findViewById(R.id.tv_info_email);
        tvInfoRole        = view.findViewById(R.id.tv_info_role);

        // Actions
        actionLogout = view.findViewById(R.id.action_logout);
    }

    private void setupActions() {
        // Nút đăng xuất
        actionLogout.setOnClickListener(v -> confirmLogout());

        // TODO: Đổi mật khẩu
        View actionChangePassword = requireView().findViewById(R.id.action_change_password);
        if (actionChangePassword != null) {
            actionChangePassword.setOnClickListener(v ->
                    showToast("Tính năng đổi mật khẩu sẽ được bổ sung sớm"));
        }
    }

    // ==================================================================
    // Load Profile
    // ==================================================================

    private void loadProfile() {
        showLoading(true);

        apiService.getMyProfile().enqueue(new Callback<UserDto>() {
            @Override
            public void onResponse(Call<UserDto> call, Response<UserDto> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    bindProfile(response.body());
                } else if (response.code() == 401) {
                    // Token hết hạn
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).redirectToLogin();
                    }
                } else {
                    showToast("Không thể tải thông tin profile");
                }
            }

            @Override
            public void onFailure(Call<UserDto> call, Throwable t) {
                showLoading(false);
                showToast(getString(R.string.err_network));
            }
        });
    }

    /**
     * Bind dữ liệu UserDto lên toàn bộ UI.
     */
    private void bindProfile(UserDto user) {
        // === Header ===
        // Avatar chữ cái đầu
        String displayName = user.getFullName() != null ? user.getFullName() : user.getUsername();
        String avatarLetter = displayName != null && !displayName.isEmpty()
                ? String.valueOf(displayName.charAt(0)).toUpperCase() : "U";
        tvAvatarLetter.setText(avatarLetter);

        tvFullName.setText(displayName != null ? displayName : "—");
        tvEmail.setText(user.getEmail() != null ? user.getEmail() : "—");

        // Tier badge
        if (user.isPremium()) {
            tvTierBadge.setText("⭐ PREMIUM");
            tvTierBadge.setTextColor(requireContext().getColor(R.color.apple_orange));
        } else if (user.isAdmin()) {
            tvTierBadge.setText("👑 ADMIN");
            tvTierBadge.setTextColor(requireContext().getColor(R.color.apple_blue));
        } else {
            tvTierBadge.setText("FREE");
        }

        // === Quota ===
        long usedBytes = user.getUsedQuota() != null ? user.getUsedQuota() : 0L;
        long maxBytes  = user.getMaxQuota()  != null ? user.getMaxQuota()  : 1L;

        int percent = user.getQuotaPercentage();
        progressQuota.setProgress(percent);
        tvQuotaPercent.setText(percent + "%");
        tvQuotaUsed.setText(FileUtils.formatSize(usedBytes) + " đã dùng");
        tvQuotaMax.setText("/ " + FileUtils.formatSize(maxBytes));

        // Đổi màu progress bar nếu gần đầy (>80% = cam, >90% = đỏ)
        if (percent >= 90) {
            progressQuota.setIndicatorColor(requireContext().getColor(R.color.apple_red));
            tvQuotaPercent.setTextColor(requireContext().getColor(R.color.apple_red));
        } else if (percent >= 80) {
            progressQuota.setIndicatorColor(requireContext().getColor(R.color.apple_orange));
            tvQuotaPercent.setTextColor(requireContext().getColor(R.color.apple_orange));
        }

        // Nút nâng cấp Premium
        if (user.isPremium() || user.isAdmin()) {
            btnUpgradePremium.setVisibility(View.GONE);
        } else {
            btnUpgradePremium.setVisibility(View.VISIBLE);
            btnUpgradePremium.setOnClickListener(v -> requestUpgrade());
        }

        // === Account info ===
        tvInfoUsername.setText(user.getUsername() != null ? user.getUsername() : "—");
        tvInfoEmail.setText(user.getEmail() != null ? user.getEmail() : "—");
        tvInfoRole.setText(buildRoleLabel(user));

        // Hiện các row info
        layoutProfileInfo.setVisibility(View.VISIBLE);
        dividerInfo.setVisibility(View.VISIBLE);
        layoutEmailInfo.setVisibility(View.VISIBLE);
        dividerEmail.setVisibility(View.VISIBLE);
        layoutRoleInfo.setVisibility(View.VISIBLE);
    }

    /**
     * Tạo chuỗi hiển thị role + tier.
     * Ví dụ: "Người dùng (Free)", "Quản trị viên"
     */
    private String buildRoleLabel(UserDto user) {
        if (user.isAdmin()) return "👑 Quản trị viên";
        if (user.isPremium()) return "⭐ Premium";
        return "Người dùng (Free)";
    }

    // ==================================================================
    // Upgrade to Premium
    // ==================================================================

    private void requestUpgrade() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Nâng cấp Premium")
                .setMessage("Gửi yêu cầu nâng cấp lên Premium để được dung lượng lưu trữ không giới hạn và nhiều tính năng hơn?")
                .setPositiveButton("Gửi yêu cầu", (d, w) -> sendUpgradeRequest())
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void sendUpgradeRequest() {
        btnUpgradePremium.setEnabled(false);
        btnUpgradePremium.setText("Đang gửi...");

        apiService.requestUpgrade().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    btnUpgradePremium.setText("✅ Yêu cầu đã được gửi");
                    showToast("Yêu cầu nâng cấp đã được gửi đến Admin!");
                } else if (response.code() == 409) {
                    btnUpgradePremium.setEnabled(true);
                    btnUpgradePremium.setText("⭐  Nâng cấp lên Premium");
                    showToast("Bạn đã gửi yêu cầu trước đó, vui lòng chờ Admin duyệt");
                } else {
                    btnUpgradePremium.setEnabled(true);
                    btnUpgradePremium.setText("⭐  Nâng cấp lên Premium");
                    showToast("❌ Không thể gửi yêu cầu");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                btnUpgradePremium.setEnabled(true);
                btnUpgradePremium.setText("⭐  Nâng cấp lên Premium");
                showToast(getString(R.string.err_network));
            }
        });
    }

    // ==================================================================
    // Logout
    // ==================================================================

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.btn_logout))
                .setMessage("Bạn có chắc muốn đăng xuất không?")
                .setPositiveButton(getString(R.string.btn_logout), (d, w) -> doLogout())
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void doLogout() {
        // Xóa session
        SessionManager.getInstance(requireContext()).clearSession();
        // Reset Retrofit client (xóa cache token)
        ApiClient.reset();

        // Về màn hình đăng nhập
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).redirectToLogin();
        }
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private void showLoading(boolean isLoading) {
        progressProfile.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showToast(String msg) {
        if (getContext() != null)
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
