package com.filemanager.android.network.dto;

import com.google.gson.annotations.SerializedName;

/** DTO yêu cầu nâng cấp Premium — GET /api/admin/upgrade-requests */
public class UpgradeRequestDto {

    @SerializedName("id")
    private Long id;

    @SerializedName("userId")
    private Long userId;

    @SerializedName("username")
    private String username;

    @SerializedName("currentTier")
    private String currentTier;

    @SerializedName("requestedTier")
    private String requestedTier;

    @SerializedName("status")
    private String status;

    @SerializedName("createdAt")
    private String createdAt;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getCurrentTier() { return currentTier; }
    public String getRequestedTier() { return requestedTier; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
}
