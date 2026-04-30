package com.filemanager.android.network.dto;

import com.google.gson.annotations.SerializedName;

/** DTO cho File/Folder metadata từ API */
public class FileMetadataDto {

    @SerializedName("id")
    private Long id;

    @SerializedName("fileName")
    private String fileName;

    @SerializedName("fileSize")
    private Long fileSize;

    @SerializedName("mimeType")
    private String mimeType;

    @SerializedName("isFolder")
    private Boolean isFolder;

    @SerializedName("parentId")
    private Long parentId;

    @SerializedName("isDeleted")
    private Boolean isDeleted;

    @SerializedName("deletedAt")
    private String deletedAt;

    @SerializedName("createdAt")
    private String createdAt;

    // --- Getters ---
    public Long getId() { return id; }
    public String getFileName() { return fileName; }
    public Long getFileSize() { return fileSize; }
    public String getMimeType() { return mimeType; }
    public Boolean getIsFolder() { return isFolder; }
    public Long getParentId() { return parentId; }
    public Boolean getIsDeleted() { return isDeleted; }
    public String getDeletedAt() { return deletedAt; }
    public String getCreatedAt() { return createdAt; }

    /** Kiểm tra nhanh đây có phải folder không */
    public boolean isFolder() {
        return Boolean.TRUE.equals(isFolder);
    }

    /** Format fileSize sang chuỗi dễ đọc (KB, MB, GB) */
    public String getFormattedSize() {
        if (fileSize == null || fileSize == 0) return "—";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        if (fileSize < 1024 * 1024 * 1024) return String.format("%.1f MB", fileSize / (1024.0 * 1024));
        return String.format("%.2f GB", fileSize / (1024.0 * 1024 * 1024));
    }
}
