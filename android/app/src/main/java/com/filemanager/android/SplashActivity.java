package com.filemanager.android;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.filemanager.android.auth.AuthRouter;
import com.filemanager.android.features.auth.LoginActivity;
import com.filemanager.android.storage.SessionManager;

/**
 * Màn hình khởi động (Splash Screen).
 * Hiển thị logo 1.5 giây, sau đó kiểm tra trạng thái đăng nhập và chuyển hướng.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Delay nhỏ để hiển thị splash, sau đó điều hướng
        new Handler(Looper.getMainLooper()).postDelayed(this::navigate, SPLASH_DELAY_MS);
    }

    private void navigate() {
        SessionManager session = SessionManager.getInstance(this);
        Intent intent;

        if (session.isLoggedIn()) {
            intent = AuthRouter.createHomeIntent(this);
        } else {
            // Chưa đăng nhập → màn hình Login
            intent = new Intent(this, LoginActivity.class);
        }

        startActivity(intent);
        finish(); // Không cho back về Splash
    }
}
