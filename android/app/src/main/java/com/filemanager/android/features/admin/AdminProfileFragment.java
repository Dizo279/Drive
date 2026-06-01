package com.filemanager.android.features.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.filemanager.android.R;
import com.filemanager.android.features.auth.LoginActivity;
import com.filemanager.android.network.ApiClient;
import com.filemanager.android.network.ApiService;
import com.filemanager.android.network.dto.UserDto;
import com.filemanager.android.storage.SessionManager;
import com.filemanager.android.network.NotificationSseClient;
import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminProfileFragment extends Fragment {

    private TextView tvAdminName;
    private TextView tvAdminEmail;
    private TextView tvAdminRole;
    private MaterialButton btnLogout;

    private ApiService apiService;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvAdminName = view.findViewById(R.id.tv_admin_name);
        tvAdminEmail = view.findViewById(R.id.tv_admin_email);
        tvAdminRole = view.findViewById(R.id.tv_admin_role);
        btnLogout = view.findViewById(R.id.btn_admin_logout);

        sessionManager = SessionManager.getInstance(requireContext());
        apiService = ApiClient.getApiService(requireContext());

        btnLogout.setOnClickListener(v -> confirmLogout());
        loadProfileInfo();
    }

    private void loadProfileInfo() {
        apiService.getMyProfile().enqueue(new Callback<UserDto>() {
            @Override
            public void onResponse(Call<UserDto> call, Response<UserDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    bindAdminProfile(response.body());
                }
            }

            @Override
            public void onFailure(Call<UserDto> call, Throwable t) {
                showToast(getString(R.string.err_network));
            }
        });
    }

    private void bindAdminProfile(UserDto user) {
        tvAdminName.setText(getString(R.string.admin_profile_name,
                user.getFullName() != null ? user.getFullName() : "—"));
        tvAdminEmail.setText(getString(R.string.admin_profile_email,
                user.getEmail() != null ? user.getEmail() : "—"));
        tvAdminRole.setText(getString(R.string.admin_profile_role,
                user.isAdmin() ? "ADMIN" : "USER"));
    }

    private void confirmLogout() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.btn_logout))
                .setMessage(getString(R.string.admin_logout_confirm))
                .setPositiveButton(getString(R.string.btn_logout), (dialog, which) -> redirectToLogin())
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void redirectToLogin() {
        sessionManager.clearSession();
        ApiClient.reset();
        NotificationSseClient.getInstance().disconnect();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showToast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
