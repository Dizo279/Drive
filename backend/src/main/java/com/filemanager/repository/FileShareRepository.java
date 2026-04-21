package com.filemanager.repository;

import com.filemanager.entity.FileShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileShareRepository extends JpaRepository<FileShare, Long> {
    Optional<FileShare> findByShareToken(String shareToken);
    List<FileShare> findByFileId(Long fileId);
    Optional<FileShare> findByFileIdAndSharedWithIsNull(Long fileId);
    Optional<FileShare> findByFileIdAndSharedWith(Long fileId, Long sharedWith);
    List<FileShare> findBySharedBy(Long sharedBy);
    List<FileShare> findBySharedWith(Long sharedWith);
}