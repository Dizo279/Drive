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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bảo vệ: nếu chưa đăng nhập thì không vào được
        if (!SessionManager.getInstance(this).isLoggedIn()) {
            redirectToLogin();
            return;
        }

        setContentView(R.layout.activity_main);

        bottomNavigation = findViewById(R.id.bottom_navigation);

        // Load fragment mặc định (Files)
        if (savedInstanceState == null) {
            loadFragment(new FilesFragment());
        }

        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_files) {
                fragment = new FilesFragment();
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
