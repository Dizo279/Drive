package com.filemanager.android.auth;

import android.content.Context;
import android.content.Intent;

import com.filemanager.android.MainActivity;
import com.filemanager.android.features.admin.AdminActivity;
import com.filemanager.android.storage.SessionManager;

/**
 * Điều hướng sau đăng nhập: Admin → AdminActivity, User → MainActivity.
 */
public final class AuthRouter {

    private AuthRouter() {}

    public static Intent createHomeIntent(Context context) {
        SessionManager session = SessionManager.getInstance(context);
        if (session.isAdmin()) {
            return new Intent(context, AdminActivity.class);
        }
        return new Intent(context, MainActivity.class);
    }

    public static void startHome(Context context) {
        Intent intent = createHomeIntent(context);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
