package com.filemanager.android.network.dto;

import com.google.gson.annotations.SerializedName;

public class UpgradeRequestDto {

    @SerializedName("id")
    private Long id;

    @SerializedName("userId")
    private Long userId;

    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("status")
    private String status; // PENDING, APPROVED, REJECTED

    @SerializedName("requestedAt")
    private String requestedAt;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getStatus() { return status; }
    public String getRequestedAt() { return requestedAt; }
}
