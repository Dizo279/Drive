package com.filemanager.android.features.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.filemanager.android.R;
import com.filemanager.android.network.ApiClient;
import com.filemanager.android.network.ApiService;
import com.filemanager.android.network.dto.RegisterRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Màn hình đăng ký tài khoản mới.
 * 5 fields: fullName, email, username, password, confirmPassword.
 * Gọi POST /api/auth/register sau khi validation client-side.
 */
public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilFullName, tilEmail, tilUsername, tilPassword, tilConfirmPassword;
    private TextInputEditText etFullName, etEmail, etUsername, etPassword, etConfirmPassword;
    private MaterialButton btnRegister;
    private LinearProgressIndicator progressRegister;
    private TextView tvError, tvGoLogin;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        initServices();
        setupListeners();
    }

    private void initViews() {
        tilFullName = findViewById(R.id.til_full_name);
        tilEmail = findViewById(R.id.til_email);
        tilUsername = findViewById(R.id.til_username);
        tilPassword = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);

        etFullName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);

        btnRegister = findViewById(R.id.btn_register);
        progressRegister = findViewById(R.id.progress_register);
        tvError = findViewById(R.id.tv_error);
        tvGoLogin = findViewById(R.id.tv_go_login);

        // Back button
        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void initServices() {
        apiService = ApiClient.getApiService(this);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> attemptRegister());

        etConfirmPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptRegister();
                return true;
            }
            return false;
        });

        tvGoLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    /** Validate toàn bộ form trước khi gọi API */
    private boolean validateForm() {
        boolean isValid = true;

        // Reset errors
        tilFullName.setError(null);
        tilEmail.setError(null);
        tilUsername.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
        hideError();

        String fullName = getTextValue(etFullName);
        String email = getTextValue(etEmail);
        String username = getTextValue(etUsername);
        String password = getTextValue(etPassword);
        String confirmPassword = getTextValue(etConfirmPassword);

        // Validate Họ tên
        if (TextUtils.isEmpty(fullName)) {
            tilFullName.setError(getString(R.string.err_full_name_required));
            isValid = false;
        } else if (fullName.length() < 2) {
            tilFullName.setError(getString(R.string.err_full_name_min));
            isValid = false;
        }

        // Validate Email
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.err_email_required));
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.err_email_invalid));
            isValid = false;
        }

        // Validate Username
        if (TextUtils.isEmpty(username)) {
            tilUsername.setError(getString(R.string.err_username_required));
            isValid = false;
        } else if (username.length() < 3) {
            tilUsername.setError(getString(R.string.err_username_min));
            isValid = false;
        }

        // Validate Password
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.err_password_required));
            isValid = false;
        } else if (password.length() < 8) {
            tilPassword.setError(getString(R.string.err_password_min));
            isValid = false;
        }

        // Validate Confirm Password
        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.err_password_mismatch));
            isValid = false;
        }

        return isValid;
    }

    private void attemptRegister() {
        if (!validateForm()) return;

        setLoading(true);

        RegisterRequest request = new RegisterRequest(
                getTextValue(etFullName),
                getTextValue(etEmail),
                getTextValue(etUsername),
                getTextValue(etPassword)
        );

        apiService.register(request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                setLoading(false);

                if (response.isSuccessful()) {
                    // Đăng ký thành công → về màn hình Login
                    showSuccessAndGoLogin();
                } else {
                    // Đọc thông báo lỗi từ server (trả về plain text)
                    try {
                        String errorMessage = response.errorBody() != null
                                ? response.errorBody().string()
                                : "Đăng ký thất bại";
                        showError(errorMessage);
                    } catch (IOException e) {
                        showError(getString(R.string.err_unknown));
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                setLoading(false);
                showError(getString(R.string.err_network));
            }
        });
    }

    private void showSuccessAndGoLogin() {
        // Hiển thị thông báo thành công rồi quay về Login
        tvError.setTextColor(getColor(R.color.apple_green));
        tvError.setText("✅ Đăng ký thành công! Đang chuyển đến trang đăng nhập...");
        tvError.setVisibility(View.VISIBLE);

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }, 1500);
    }

    private void setLoading(boolean isLoading) {
        btnRegister.setEnabled(!isLoading);
        progressRegister.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnRegister.setText(isLoading ? getString(R.string.loading) : getString(R.string.btn_register));
    }

    private void showError(String message) {
        tvError.setTextColor(getColor(R.color.apple_red));
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvError.setVisibility(View.GONE);
    }

    private String getTextValue(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
