package com.filemanager.android.features.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.filemanager.android.R;
import com.filemanager.android.features.auth.LoginActivity;
import com.filemanager.android.network.NotificationSseClient;
import com.filemanager.android.storage.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Màn hình chính dành riêng cho Admin — chỉ bao gồm dashboard, thông báo và thông tin cá nhân.
 */
public class AdminActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = SessionManager.getInstance(this);
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }
        if (!sessionManager.isAdmin()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_admin);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_admin);
        String username = sessionManager.getUsername();
        toolbar.setSubtitle(username != null ? "Xin chào, " + username : "File Manager Admin");
        toolbar.inflateMenu(R.menu.admin_toolbar_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_logout) {
                confirmLogout();
                return true;
            }
            return false;
        });

        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation_admin);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_admin_home) {
                toolbar.setTitle("👑 Quản trị");
                loadFragment(new AdminHomeFragment());
                return true;
            } else if (itemId == R.id.nav_admin_notifications) {
                toolbar.setTitle("🔔 Thông báo");
                loadFragment(new com.filemanager.android.features.notifications.NotificationsFragment());
                return true;
            } else if (itemId == R.id.nav_admin_profile) {
                toolbar.setTitle("👤 Cá nhân");
                loadFragment(new AdminProfileFragment());
                return true;
            }
            return false;
        });

        bottomNavigation.setSelectedItemId(R.id.nav_admin_home);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container_admin, fragment)
                .commit();
    }

    private void confirmLogout() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.btn_logout))
                .setMessage("Đăng xuất khỏi tài khoản quản trị?")
                .setPositiveButton(getString(R.string.btn_logout), (dialog, which) -> redirectToLogin())
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void redirectToLogin() {
        sessionManager.clearSession();
        com.filemanager.android.network.ApiClient.reset();
        NotificationSseClient.getInstance().disconnect();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
