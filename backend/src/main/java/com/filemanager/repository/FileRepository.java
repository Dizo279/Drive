package com.filemanager.repository;

import com.filemanager.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<FileMetadata, Long> {
    List<FileMetadata> findByOwnerId(Long ownerId);
}