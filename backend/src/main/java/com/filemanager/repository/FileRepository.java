package com.filemanager.repository;

import com.filemanager.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
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
}