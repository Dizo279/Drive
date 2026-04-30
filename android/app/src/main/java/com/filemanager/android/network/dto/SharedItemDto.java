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

    public Long getShareId() { return shareId; }
    public Long getFileId() { return fileId; }
    public String getFileName() { return fileName; }
    public String getTargetEmail() { return targetEmail; }
    public String getExpiresAt() { return expiresAt; }
    public String getShareToken() { return shareToken; }
}
