package com.filemanager.android.features.profile;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewOutlineProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.filemanager.android.MainActivity;
import com.filemanager.android.R;
import com.filemanager.android.network.ApiClient;
import com.filemanager.android.network.ApiService;
import com.filemanager.android.network.dto.ProfileUpdateRequest;
import com.filemanager.android.network.dto.UserDto;
import com.filemanager.android.storage.SessionManager;
import com.filemanager.android.utils.FileUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragment Hồ sơ cá nhân.
 *
 * Tính năng:
 * - Hiển thị avatar (chữ cái đầu tên hoặc ảnh), fullName, email, role
 * - Storage quota với ProgressBar và số liệu đã dùng / tổng
 * - Nút Nâng cấp Premium (POST /api/users/upgrade-request)
 * - Chỉnh sửa thông tin (fullName, username, email) với dialog
 * - Đổi mật khẩu với dialog
 * - Chọn ảnh avatar từ gallery và upload dưới dạng Base64
 * - Nút Đăng xuất với xác nhận
 */
public class ProfileFragment extends Fragment {

    // === Header views ===
    private TextView tvAvatarLetter;
    private ImageView ivAvatar;
    private FrameLayout layoutAvatar;
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
    private LinearLayout actionEditProfile;
    private LinearLayout actionChangePassword;
    private LinearLayout actionLogout;

    private ApiService apiService;
    private SessionManager sessionManager;
    /** Cache dữ liệu profile hiện tại để pre-fill vào dialog chỉnh sửa */
    private UserDto currentUser;

    /** Launcher chọn ảnh từ gallery */
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Đăng ký launcher trước onCreateView
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            handleAvatarSelected(imageUri);
                        }
                    }
                }
        );
    }

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
        sessionManager = SessionManager.getInstance(requireContext());

        setupAvatarClip();
        setupActions();
        showCachedAvatarIfAny();
        loadProfile();
    }

    private void setupAvatarClip() {
        ivAvatar.setClipToOutline(true);
        ivAvatar.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
    }

    /** Hiển thị avatar đã lưu local trước khi API trả về (tránh nhấp nháy / mất ảnh). */
    private void showCachedAvatarIfAny() {
        String cached = sessionManager.getAvatarUrl();
        if (cached != null && !cached.isEmpty()) {
            displayAvatarUrl(cached, currentUser != null && currentUser.getFullName() != null
                    ? currentUser.getFullName() : sessionManager.getUsername());
        }
    }

    private void initViews(View view) {
        // Header
        tvAvatarLetter   = view.findViewById(R.id.tv_avatar_letter);
        ivAvatar         = view.findViewById(R.id.iv_avatar);
        layoutAvatar     = view.findViewById(R.id.layout_avatar);
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
        actionEditProfile    = view.findViewById(R.id.action_edit_profile);
        actionChangePassword = view.findViewById(R.id.action_change_password);
        actionLogout         = view.findViewById(R.id.action_logout);
    }

    private void setupActions() {
        actionLogout.setOnClickListener(v -> confirmLogout());
        actionChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        actionEditProfile.setOnClickListener(v -> showEditProfileDialog());
        layoutAvatar.setOnClickListener(v -> openImagePicker());
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
                    currentUser = response.body();
                    bindProfile(currentUser);
                } else if (response.code() == 401) {
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
        String displayName = user.getFullName() != null ? user.getFullName() : user.getUsername();

        // Avatar: ưu tiên hiện ảnh, fallback chữ cái đầu
        String avatarUrl = user.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            sessionManager.saveAvatarUrl(avatarUrl);
            displayAvatarUrl(avatarUrl, displayName);
        } else {
            sessionManager.saveAvatarUrl(null);
            showLetterAvatar(displayName);
        }

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

    private void displayAvatarUrl(String avatarUrl, String displayName) {
        tvAvatarLetter.setVisibility(View.GONE);
        ivAvatar.setVisibility(View.VISIBLE);
        if (avatarUrl.startsWith("data:image")) {
            try {
                String base64 = avatarUrl.substring(avatarUrl.indexOf(",") + 1);
                byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                Glide.with(this).load(bmp).circleCrop().into(ivAvatar);
            } catch (Exception e) {
                showLetterAvatar(displayName);
            }
        } else {
            Glide.with(this).load(avatarUrl).circleCrop().into(ivAvatar);
        }
    }

    private void showLetterAvatar(String displayName) {
        ivAvatar.setVisibility(View.GONE);
        tvAvatarLetter.setVisibility(View.VISIBLE);
        String letter = displayName != null && !displayName.isEmpty()
                ? String.valueOf(displayName.charAt(0)).toUpperCase() : "U";
        tvAvatarLetter.setText(letter);
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
    // Avatar — Chọn ảnh từ Gallery và upload Base64
    // ==================================================================

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    /**
     * Xử lý ảnh avatar đã chọn:
     * 1. Đọc InputStream → Bitmap
     * 2. Resize xuống max 256px để tiết kiệm dung lượng
     * 3. Encode sang Base64 data URI
     * 4. Gửi lên backend qua PUT /api/users/profile
     */
    private void handleAvatarSelected(Uri imageUri) {
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(imageUri);
            if (is == null) {
                showToast("Không thể đọc ảnh");
                return;
            }

            Bitmap original = BitmapFactory.decodeStream(is);
            is.close();

            if (original == null) {
                showToast("Ảnh không hợp lệ");
                return;
            }

            // Resize xuống max 256x256
            Bitmap resized = resizeBitmap(original, 256);

            // Encode Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            String base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
            String dataUri = "data:image/jpeg;base64," + base64;

            sessionManager.saveAvatarUrl(dataUri);
            Glide.with(this).load(resized).circleCrop().into(ivAvatar);
            tvAvatarLetter.setVisibility(View.GONE);
            ivAvatar.setVisibility(View.VISIBLE);

            uploadAvatar(dataUri);

        } catch (Exception e) {
            showToast("Lỗi xử lý ảnh: " + e.getMessage());
        }
    }

    private Bitmap resizeBitmap(Bitmap src, int maxSize) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= maxSize && h <= maxSize) return src;

        float ratio = Math.min((float) maxSize / w, (float) maxSize / h);
        return Bitmap.createScaledBitmap(src, (int)(w * ratio), (int)(h * ratio), true);
    }

    private void uploadAvatar(String dataUri) {
        showToast("Đang cập nhật avatar...");

        ProfileUpdateRequest req = new ProfileUpdateRequest().setAvatarUrl(dataUri);
        apiService.updateProfile(req).enqueue(new Callback<UserDto>() {
            @Override
            public void onResponse(Call<UserDto> call, Response<UserDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                    if (currentUser.getAvatarUrl() != null) {
                        sessionManager.saveAvatarUrl(currentUser.getAvatarUrl());
                    }
                    bindProfile(currentUser);
                    showToast("✅ Cập nhật avatar thành công!");
                } else {
                    showToast("❌ Không thể cập nhật avatar");
                    // Revert về trạng thái cũ
                    if (currentUser != null) bindProfile(currentUser);
                }
            }

            @Override
            public void onFailure(Call<UserDto> call, Throwable t) {
                showToast(getString(R.string.err_network));
                if (currentUser != null) bindProfile(currentUser);
            }
        });
    }

    // ==================================================================
    // Edit Profile — Dialog chỉnh sửa FullName, Username, Email
    // ==================================================================

    private void showEditProfileDialog() {
        if (currentUser == null) {
            showToast("Đang tải thông tin, vui lòng chờ...");
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_profile, null);

        TextInputEditText etFullName = dialogView.findViewById(R.id.et_edit_fullname);
        TextInputEditText etUsername = dialogView.findViewById(R.id.et_edit_username);
        TextInputEditText etEmail   = dialogView.findViewById(R.id.et_edit_email);
        TextInputEditText etPassword = dialogView.findViewById(R.id.et_current_password);
        TextInputLayout tilPassword = dialogView.findViewById(R.id.til_current_password);
        TextView tvPasswordHint     = dialogView.findViewById(R.id.tv_password_hint);

        // Pre-fill giá trị hiện tại
        etFullName.setText(currentUser.getFullName());
        etUsername.setText(currentUser.getUsername());
        etEmail.setText(currentUser.getEmail());

        // TextWatcher: hiện trường mật khẩu khi thay đổi username hoặc email
        TextWatcher sensitiveWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                boolean usernameChanged = !getText(etUsername).equals(
                        currentUser.getUsername() != null ? currentUser.getUsername() : "");
                boolean emailChanged = !getText(etEmail).equals(
                        currentUser.getEmail() != null ? currentUser.getEmail() : "");

                boolean needPassword = usernameChanged || emailChanged;
                tilPassword.setVisibility(needPassword ? View.VISIBLE : View.GONE);
                tvPasswordHint.setVisibility(needPassword ? View.VISIBLE : View.GONE);
            }
        };
        etUsername.addTextChangedListener(sensitiveWatcher);
        etEmail.addTextChangedListener(sensitiveWatcher);

        new AlertDialog.Builder(requireContext())
                .setTitle("✏️ Chỉnh sửa thông tin")
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String fullName = getText(etFullName);
                    String username = getText(etUsername);
                    String email    = getText(etEmail);
                    String password = getText(etPassword);

                    // Validation cơ bản
                    if (fullName.isEmpty()) {
                        showToast("Họ tên không được để trống");
                        return;
                    }

                    boolean usernameChanged = !username.equals(
                            currentUser.getUsername() != null ? currentUser.getUsername() : "");
                    boolean emailChanged = !email.equals(
                            currentUser.getEmail() != null ? currentUser.getEmail() : "");

                    if ((usernameChanged || emailChanged) && password.isEmpty()) {
                        showToast("Cần nhập mật khẩu hiện tại để đổi Username/Email");
                        return;
                    }

                    // Build request
                    ProfileUpdateRequest req = new ProfileUpdateRequest()
                            .setFullName(fullName);

                    if (usernameChanged) req.setUsername(username);
                    if (emailChanged) req.setEmail(email);
                    if (!password.isEmpty()) req.setCurrentPassword(password);

                    submitProfileUpdate(req);
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    // ==================================================================
    // Change Password — Dialog đổi mật khẩu
    // ==================================================================

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_change_password, null);

        TextInputEditText etOldPw    = dialogView.findViewById(R.id.et_old_password);
        TextInputEditText etNewPw    = dialogView.findViewById(R.id.et_new_password);
        TextInputEditText etConfirm  = dialogView.findViewById(R.id.et_confirm_password);
        TextInputLayout tilOld       = dialogView.findViewById(R.id.til_old_password);
        TextInputLayout tilNew       = dialogView.findViewById(R.id.til_new_password);
        TextInputLayout tilConfirm   = dialogView.findViewById(R.id.til_confirm_password);

        new AlertDialog.Builder(requireContext())
                .setTitle("🔑 Đổi mật khẩu")
                .setView(dialogView)
                .setPositiveButton("Đổi mật khẩu", (dialog, which) -> {
                    String oldPw    = getText(etOldPw);
                    String newPw    = getText(etNewPw);
                    String confirm  = getText(etConfirm);

                    // Validation
                    if (oldPw.isEmpty()) {
                        showToast("Vui lòng nhập mật khẩu hiện tại");
                        return;
                    }
                    if (newPw.length() < 8) {
                        showToast("Mật khẩu mới phải có ít nhất 8 ký tự");
                        return;
                    }
                    if (!newPw.equals(confirm)) {
                        showToast("Mật khẩu xác nhận không khớp");
                        return;
                    }

                    ProfileUpdateRequest req = new ProfileUpdateRequest()
                            .setCurrentPassword(oldPw)
                            .setNewPassword(newPw);

                    submitProfileUpdate(req);
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    // ==================================================================
    // Gửi cập nhật profile lên server
    // ==================================================================

    private void submitProfileUpdate(ProfileUpdateRequest req) {
        showLoading(true);

        apiService.updateProfile(req).enqueue(new Callback<UserDto>() {
            @Override
            public void onResponse(Call<UserDto> call, Response<UserDto> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                    bindProfile(currentUser);
                    showToast("✅ Cập nhật thành công!");
                } else {
                    // Đọc lỗi từ body
                    String errorMsg = "❌ Cập nhật thất bại";
                    try {
                        if (response.errorBody() != null) {
                            String body = response.errorBody().string();
                            if (body.contains("Mật khẩu")) {
                                errorMsg = "Mật khẩu hiện tại không đúng!";
                            } else if (body.contains("error")) {
                                // Trích xuất thông báo lỗi
                                int start = body.indexOf("\"error\"") + 9;
                                int end = body.indexOf("\"", start);
                                if (end > start) errorMsg = body.substring(start, end);
                            }
                        }
                    } catch (Exception ignored) {}
                    showToast(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<UserDto> call, Throwable t) {
                showLoading(false);
                showToast(getString(R.string.err_network));
            }
        });
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

    /** Lấy text từ EditText, đã trim. */
    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
