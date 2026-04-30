package com.filemanager.android.network.dto;

import com.google.gson.annotations.SerializedName;

/** DTO cho request đăng ký — POST /api/auth/register */
public class RegisterRequest {

    @SerializedName("fullName")
    private String fullName;

    @SerializedName("email")
    private String email;

    @SerializedName("username")
    private String username;

    @SerializedName("password")
    private String password;

    public RegisterRequest(String fullName, String email, String username, String password) {
        this.fullName = fullName;
        this.email = email;
        this.username = username;
        this.password = password;
    }

    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
