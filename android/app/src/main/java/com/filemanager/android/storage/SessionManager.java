package com.filemanager.android.storage;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Quản lý phiên đăng nhập người dùng thông qua SharedPreferences.
 * Lưu trữ JWT token và thông tin cơ bản của user.
 */
public class SessionManager {

    private static final String PREF_NAME = "FileManagerSession";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_USER_ID = "user_id";

    private final SharedPreferences prefs;
    private static SessionManager instance;

    private SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /** Singleton - dùng chung toàn app */
    public static SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    /**
     * Lưu thông tin đăng nhập sau khi login thành công.
     * @param token    JWT token từ API
     * @param username Tên đăng nhập
     */
    public void saveSession(String token, String username) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USERNAME, username)
                .apply();
    }

    /** Lấy JWT token để gắn vào API request header. */
    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    /** Lấy username đang đăng nhập. */
    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    /** Kiểm tra user đã đăng nhập chưa (token tồn tại). */
    public boolean isLoggedIn() {
        return getToken() != null;
    }

    /** Xóa toàn bộ session khi Đăng xuất. */
    public void clearSession() {
        prefs.edit().clear().apply();
    }
}
