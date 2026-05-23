package com.filemanager.android.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO gửi lên PUT /api/users/profile.
 * Chỉ set các trường cần thay đổi, trường null sẽ bị backend bỏ qua.
 */
public class ProfileUpdateRequest {

    @SerializedName("fullName")
    private String fullName;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("currentPassword")
    private String currentPassword;

    @SerializedName("newPassword")
    private String newPassword;

    // --- Setters (Builder pattern) ---

    public ProfileUpdateRequest setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public ProfileUpdateRequest setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        return this;
    }

    public ProfileUpdateRequest setUsername(String username) {
        this.username = username;
        return this;
    }

    public ProfileUpdateRequest setEmail(String email) {
        this.email = email;
        return this;
    }

    public ProfileUpdateRequest setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
        return this;
    }

    public ProfileUpdateRequest setNewPassword(String newPassword) {
        this.newPassword = newPassword;
        return this;
    }

    // --- Getters ---
    public String getFullName() { return fullName; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getCurrentPassword() { return currentPassword; }
    public String getNewPassword() { return newPassword; }
}
