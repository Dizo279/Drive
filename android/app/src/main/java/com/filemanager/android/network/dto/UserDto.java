package com.filemanager.android.network.dto;

import com.google.gson.annotations.SerializedName;

/** DTO thông tin người dùng — GET /api/users/me */
public class UserDto {

    @SerializedName("id")
    private Long id;

    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("fullName")
    private String fullName;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("role")
    private String role;        // "USER" hoặc "ADMIN"

    @SerializedName("tier")
    private String tier;        // "FREE" hoặc "PREMIUM"

    @SerializedName("usedQuota")
    private Long usedQuota;

    @SerializedName("maxQuota")
    private Long maxQuota;

    // --- Getters ---
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getRole() { return role; }
    public String getTier() { return tier; }
    public Long getUsedQuota() { return usedQuota; }
    public Long getMaxQuota() { return maxQuota; }

    /** Tính phần trăm quota đã dùng */
    public int getQuotaPercentage() {
        if (maxQuota == null || maxQuota == 0) return 0;
        long used = usedQuota != null ? usedQuota : 0L;
        return (int) ((used * 100) / maxQuota);
    }

    public boolean isAdmin() { return "ADMIN".equals(role); }
    public boolean isPremium() { return "PREMIUM".equals(tier); }
}
