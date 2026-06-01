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
    private static final String KEY_ROLE = "role";
    private static final String KEY_AVATAR_URL = "avatar_url";

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
    public void saveSession(String token, String username, String role) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USERNAME, username)
                .putString(KEY_ROLE, role != null ? role : "USER")
                .apply();
    }

    /** Giữ tương thích cũ — mặc định USER */
    public void saveSession(String token, String username) {
        saveSession(token, username, "USER");
    }

    /** Lấy JWT token để gắn vào API request header. */
    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    /** Lấy username đang đăng nhập. */
    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, null);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(getRole());
    }

    /** Kiểm tra user đã đăng nhập chưa (token tồn tại). */
    public boolean isLoggedIn() {
        return getToken() != null;
    }

    /** Lưu URL avatar (data URI hoặc URL) để hiển thị ngay khi mở lại Profile. */
    public void saveAvatarUrl(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            prefs.edit().remove(KEY_AVATAR_URL).apply();
        } else {
            prefs.edit().putString(KEY_AVATAR_URL, avatarUrl).apply();
        }
    }

    public String getAvatarUrl() {
        return prefs.getString(KEY_AVATAR_URL, null);
    }

    /** Xóa toàn bộ session khi Đăng xuất. */
    public void clearSession() {
        prefs.edit().clear().apply();
    }
}
