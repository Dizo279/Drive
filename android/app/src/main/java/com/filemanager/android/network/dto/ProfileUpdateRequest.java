package com.filemanager.android.network.dto;

import com.google.gson.annotations.SerializedName;

/** DTO gửi lên để cập nhật profile người dùng. */
public class ProfileUpdateRequest {

    @SerializedName("fullName")
    public String fullName;

    @SerializedName("email")
    public String email;

    @SerializedName("avatarUrl")
    public String avatarUrl;

    @SerializedName("currentPassword")
    public String currentPassword;
}
