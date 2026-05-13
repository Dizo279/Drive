package com.filemanager.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "files")
public class FileMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_folder")
    private Boolean isFolder = false;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Getters
    public Long getId() { return id; }
    public Long getOwnerId() { return ownerId; }
    public String getFileName() { return fileName; }
    public String getFilePath() { return filePath; }
    public String getMimeType() { return mimeType; }
    public Long getFileSize() { return fileSize; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public Boolean getIsFolder() { return isFolder; }
    public Long getParentId() { return parentId; }
    public Boolean getIsDeleted() { return isDeleted; }
    public LocalDateTime getDeletedAt() { return deletedAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setIsFolder(Boolean isFolder) { this.isFolder = isFolder; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}