package com.filemanager.android.network.dto;

import com.google.gson.annotations.SerializedName;

/** DTO thông báo — GET /api/notifications */
public class NotificationDto {

    @SerializedName("id")
    private Long id;

    @SerializedName("type")
    private String type;        // "FILE_SHARED", "UPGRADE_REQUEST", etc.

    @SerializedName("message")
    private String message;

    @SerializedName("targetUrl")
    private String targetUrl;

    @SerializedName("isRead")
    private Boolean isRead;

    @SerializedName("createdAt")
    private String createdAt;

    // --- Getters ---
    public Long getId() { return id; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public String getTargetUrl() { return targetUrl; }
    public Boolean getIsRead() { return isRead; }
    public String getCreatedAt() { return createdAt; }
    public boolean isRead() { return Boolean.TRUE.equals(isRead); }
}
