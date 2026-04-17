package com.filemanager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;
import java.io.IOException;

@Service
public class StorageService {

    @Value("${app.storage.upload-dir}")
    private String uploadDir;

    public String storeFile(InputStream inputStream, String originalFilename) throws Exception {
        Path dirPath = Paths.get(uploadDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath); // Tự động tạo thư mục nếu chưa có
        }
        
        // Lấy phần mở rộng của file (ví dụ: .pdf, .jpg)
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex);
        }
        
        // Tạo tên file ngẫu nhiên để không bị ghi đè
        String uniqueName = UUID.randomUUID().toString() + extension;
        Path targetLocation = dirPath.resolve(uniqueName);
        
        // Ghi file xuống disk
        Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        return uniqueName;
    }

    public long getFileSize(String fileName) throws Exception {
        return Files.size(Paths.get(uploadDir).resolve(fileName));
    }

    public String getUploadDir() {
        return this.uploadDir;
    }

    public void deletePhysicalFile(String fileName) {
        try {
            Path filePath = Paths.get(this.uploadDir).resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("Không thể xóa file vật lý: " + e.getMessage());
        }
    }
}