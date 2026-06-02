package com.filemanager.android.network.dto;

import com.google.gson.annotations.SerializedName;

public class AdminStatsDto {

    @SerializedName("totalUsers")
    private Long totalUsers;

    @SerializedName("totalStorageUsed")
    private Long totalStorageUsed;

    public Long getTotalUsers() { return totalUsers != null ? totalUsers : 0L; }
    public Long getTotalStorageUsed() { return totalStorageUsed != null ? totalStorageUsed : 0L; }
}
