package com.filemanager.service;

import com.filemanager.entity.FileMetadata;
import com.filemanager.entity.User;
import com.filemanager.repository.FileRepository;
import com.filemanager.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final StorageService storageService;

    public TrashCleanupService(FileRepository fileRepository, UserRepository userRepository, StorageService storageService) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
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

            // Trừ dung lượng quota khi xóa vĩnh viễn
            try {
                Long ownerId = item.getOwnerId();
                User user = ownerId != null ? userRepository.findById(ownerId).orElse(null) : null;
                if (user != null && item.getFileSize() != null) {
                    long currentUsed = user.getUsedQuota() != null ? user.getUsedQuota() : 0L;
                    user.setUsedQuota(Math.max(0, currentUsed - item.getFileSize()));
                    userRepository.save(user);
                }
            } catch (Exception e) {
                System.err.println("Không thể cập nhật quota khi purge trash: ownerId=" + item.getOwnerId());
            }
        }

        fileRepository.delete(item);
    }
}

