package com.filemanager.repository;

import com.filemanager.entity.FileShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FileShareRepository extends JpaRepository<FileShare, Long> {
    Optional<FileShare> findByShareToken(String shareToken);
    Optional<FileShare> findByFileId(Long fileId);
}