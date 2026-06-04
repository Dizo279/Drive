package com.filemanager.android;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.filemanager.android.features.auth.LoginActivity;
import com.filemanager.android.features.files.FilesFragment;
import com.filemanager.android.features.notifications.NotificationsFragment;
import com.filemanager.android.features.profile.ProfileFragment;
import com.filemanager.android.features.shared.SharedFragment;
import com.filemanager.android.features.trash.TrashFragment;
import com.filemanager.android.storage.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Activity chính chứa Bottom Navigation và các Fragment.
 * Kiểm tra session khi khởi động — redirect về Login nếu chưa đăng nhập.
 */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private com.filemanager.android.network.AdminSseClient adminSseClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!SessionManager.getInstance(this).isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        bottomNavigation = findViewById(R.id.bottom_navigation);

        boolean isAdmin = SessionManager.getInstance(this).isAdmin();

        if (isAdmin) {
            bottomNavigation.getMenu().removeItem(R.id.nav_shared);
            bottomNavigation.getMenu().removeItem(R.id.nav_trash);
            
            // Start listening for SSE notifications
            adminSseClient = new com.filemanager.android.network.AdminSseClient(this);
            adminSseClient.startListening();
        }

        // Load fragment mặc định (Files or Admin Dashboard)
        if (savedInstanceState == null) {
            if (isAdmin) {
                loadFragment(new com.filemanager.android.features.admin.AdminDashboardFragment());
            } else {
                loadFragment(new FilesFragment());
            }
        }

        setupBottomNavigation(isAdmin);
        requestNotificationPermission();
    }

    private void requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void setupBottomNavigation(boolean isAdmin) {
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_files) {
                if (isAdmin) {
                    fragment = new com.filemanager.android.features.admin.AdminDashboardFragment();
                } else {
                    fragment = new FilesFragment();
                }
            } else if (itemId == R.id.nav_shared) {
                fragment = new SharedFragment();
            } else if (itemId == R.id.nav_trash) {
                fragment = new TrashFragment();
            } else if (itemId == R.id.nav_notifications) {
                fragment = new NotificationsFragment();
            } else if (itemId == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    /**
     * Khi nhấn Back: nếu FilesFragment đang ở thư mục con → back về thư mục cha.
     * Nếu đã ở root → thoát app bình thường.
     */
    @Override
    public void onBackPressed() {
        androidx.fragment.app.Fragment currentFragment =
                getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof com.filemanager.android.features.files.FilesFragment) {
            com.filemanager.android.features.files.FilesFragment filesFragment =
                    (com.filemanager.android.features.files.FilesFragment) currentFragment;
            if (filesFragment.onBackPressed()) return; // Fragment xử lý
        }
        super.onBackPressed();
    }

    /** Xóa session và về màn hình Login */
    public void redirectToLogin() {
        SessionManager.getInstance(this).clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
