package com.filemanager.android.network.dto;

import com.google.gson.annotations.SerializedName;

/** DTO cho mục đã chia sẻ — shared-by-me / shared-with-me */
public class SharedItemDto {

    @SerializedName("shareId")
    private Long shareId;

    @SerializedName("fileId")
    private Long fileId;

    @SerializedName("fileName")
    private String fileName;

    @SerializedName("targetEmail")
    private String targetEmail;

    @SerializedName("expiresAt")
    private String expiresAt;

    @SerializedName("shareToken")
    private String shareToken;

    public SharedItemDto() {}

    public SharedItemDto(Long shareId, Long fileId, String fileName, String targetEmail, String expiresAt, String shareToken) {
        this.shareId = shareId;
        this.fileId = fileId;
        this.fileName = fileName;
        this.targetEmail = targetEmail;
        this.expiresAt = expiresAt;
        this.shareToken = shareToken;
    }

    public Long getShareId() { return shareId; }
    public Long getFileId() { return fileId; }
    public String getFileName() { return fileName; }
    public String getTargetEmail() { return targetEmail; }
    public String getExpiresAt() { return expiresAt; }
    public String getShareToken() { return shareToken; }

    public void setShareId(Long shareId) { this.shareId = shareId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setTargetEmail(String targetEmail) { this.targetEmail = targetEmail; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }
}
