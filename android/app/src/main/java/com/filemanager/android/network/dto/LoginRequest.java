package com.filemanager.android.network.dto;

import com.google.gson.annotations.SerializedName;

/** DTO cho request đăng nhập — POST /api/auth/login */
public class LoginRequest {

    @SerializedName("username")
    private String username;

    @SerializedName("password")
    private String password;

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
