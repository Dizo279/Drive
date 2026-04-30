package com.filemanager.android.features.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.filemanager.android.MainActivity;
import com.filemanager.android.R;
import com.filemanager.android.network.ApiClient;
import com.filemanager.android.network.ApiService;
import com.filemanager.android.network.dto.LoginRequest;
import com.filemanager.android.network.dto.LoginResponse;
import com.filemanager.android.storage.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Màn hình đăng nhập.
 * Gọi POST /api/auth/login → lưu JWT token → vào MainActivity.
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilUsername, tilPassword;
    private TextInputEditText etUsername, etPassword;
    private MaterialButton btnLogin;
    private LinearProgressIndicator progressLogin;
    private TextView tvError, tvGoRegister;

    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        initServices();
        setupListeners();
    }

    private void initViews() {
        tilUsername = findViewById(R.id.til_username);
        tilPassword = findViewById(R.id.til_password);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        progressLogin = findViewById(R.id.progress_login);
        tvError = findViewById(R.id.tv_error);
        tvGoRegister = findViewById(R.id.tv_go_register);
    }

    private void initServices() {
        apiService = ApiClient.getApiService(this);
        sessionManager = SessionManager.getInstance(this);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());

        // Nhấn Done trên bàn phím → đăng nhập
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });

        // Chuyển sang màn hình Register
        tvGoRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    /** Validate input và gọi API login */
    private void attemptLogin() {
        // Reset lỗi cũ
        tilUsername.setError(null);
        tilPassword.setError(null);
        hideError();

        String username = getTextValue(etUsername);
        String password = getTextValue(etPassword);

        // Validate phía client
        if (TextUtils.isEmpty(username)) {
            tilUsername.setError(getString(R.string.err_username_required));
            etUsername.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.err_password_required));
            etPassword.requestFocus();
            return;
        }

        setLoading(true);

        // Gọi API
        LoginRequest request = new LoginRequest(username, password);
        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    // Lưu session
                    sessionManager.saveSession(
                            loginResponse.getToken(),
                            loginResponse.getUsername()
                    );

                    // Vào màn hình chính
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                } else if (response.code() == 401) {
                    showError("Sai tên đăng nhập hoặc mật khẩu");
                } else {
                    showError("Lỗi đăng nhập: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                setLoading(false);
                showError(getString(R.string.err_network));
            }
        });
    }

    private void setLoading(boolean isLoading) {
        btnLogin.setEnabled(!isLoading);
        progressLogin.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setText(isLoading ? getString(R.string.loading) : getString(R.string.btn_login));
    }

    private void showError(String message) {
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
