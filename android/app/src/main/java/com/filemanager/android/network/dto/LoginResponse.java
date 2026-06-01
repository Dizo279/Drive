package com.filemanager.android.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO cho response đăng nhập thành công.
 * Backend trả về: {"token": "...", "username": "..."}
 */
public class LoginResponse {

    @SerializedName("token")
    private String token;

    @SerializedName("username")
    private String username;

    @SerializedName("role")
    private String role;

    public String getToken() { return token; }
    public String getUsername() { return username; }
    public String getRole() { return role; }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
