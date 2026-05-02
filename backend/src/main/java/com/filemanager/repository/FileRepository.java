package com.filemanager.repository;

import com.filemanager.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<FileMetadata, Long> {
    List<FileMetadata> findByOwnerIdAndIsDeletedFalse(Long ownerId);
    List<FileMetadata> findByOwnerIdAndParentIdAndIsDeletedFalse(Long ownerId, Long parentId);
    List<FileMetadata> findByOwnerIdAndParentIdIsNullAndIsDeletedFalse(Long ownerId);
    List<FileMetadata> findByParentIdAndIsDeletedFalse(Long parentId);

    List<FileMetadata> findByOwnerIdAndIsDeletedTrueOrderByDeletedAtDesc(Long ownerId);
    List<FileMetadata> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime cutoff);
    List<FileMetadata> findByParentId(Long parentId);

    /**
     * Lấy các thư mục/file nằm ở thư mục gốc của Trash.
     * Tức là những item bị xóa (isDeleted = true) mà cha của nó (nếu có) KHÔNG nằm trong Trash.
     * Giúp cho UI không bị rải rác các file con khi xóa cả folder.
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.ownerId = :ownerId AND f.isDeleted = true " +
           "AND (f.parentId IS NULL OR NOT EXISTS (SELECT 1 FROM FileMetadata p WHERE p.id = f.parentId AND p.isDeleted = true)) " +
           "ORDER BY f.deletedAt DESC")
    List<FileMetadata> findRootTrashItemsByOwner(@Param("ownerId") Long ownerId);

    /**
     * Tính tổng dung lượng thực tế (bytes) của tất cả file không bị xóa và không phải folder.
     * Đây là nguồn sự thật duy nhất cho quota, tránh race condition khi cập nhật user.usedQuota.
     */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileMetadata f " +
           "WHERE f.ownerId = :ownerId AND f.isDeleted = false AND f.isFolder = false")
    long sumUsedQuotaByOwner(@Param("ownerId") Long ownerId);
}