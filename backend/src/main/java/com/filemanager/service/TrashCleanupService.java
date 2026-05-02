package com.filemanager.service;

import com.filemanager.entity.FileMetadata;
import com.filemanager.repository.FileRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TrashCleanupService {

    private static final int TRASH_RETENTION_DAYS = 30;

    private final FileRepository fileRepository;
    private final StorageService storageService;

    public TrashCleanupService(FileRepository fileRepository, StorageService storageService) {
        this.fileRepository = fileRepository;
        this.storageService = storageService;
    }

    // Chạy 03:00 mỗi ngày
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTrash() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(TRASH_RETENTION_DAYS);
        List<FileMetadata> expired = fileRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoff);

        for (FileMetadata item : expired) {
            hardDeleteItem(item);
        }
    }

    private void hardDeleteItem(FileMetadata item) {
        // Folder không có file vật lý; children sẽ tự bị purge khi đến hạn
        if (!Boolean.TRUE.equals(item.getIsFolder())) {
            try {
                if (item.getFilePath() != null && !item.getFilePath().isBlank()) {
                    java.nio.file.Path physicalPath = Paths.get(storageService.getUploadDir()).resolve(item.getFilePath());
                    Files.deleteIfExists(physicalPath);
                }
            } catch (Exception e) {
                System.err.println("Không thể xóa file vật lý trong purge trash: " + item.getFilePath());
            }
            // Không cần trừ quota ở đây — quota được tính trực tiếp từ DB qua SUM query.
        }

        fileRepository.delete(item);
    }

}

