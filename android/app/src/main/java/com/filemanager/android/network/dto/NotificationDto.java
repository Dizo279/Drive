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

    // --- Constructors ---
    public NotificationDto() {}

    public NotificationDto(Long id, String type, String message, String targetUrl, Boolean isRead, String createdAt) {
        this.id = id;
        this.type = type;
        this.message = message;
        this.targetUrl = targetUrl;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    // --- Getters ---
    public Long getId() { return id; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public String getTargetUrl() { return targetUrl; }
    public Boolean getIsRead() { return isRead; }
    public String getCreatedAt() { return createdAt; }
    public boolean isRead() { return Boolean.TRUE.equals(isRead); }

    // --- Setters ---
    public void setId(Long id) { this.id = id; }
    public void setType(String type) { this.type = type; }
    public void setMessage(String message) { this.message = message; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
