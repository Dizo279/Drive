package com.filemanager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class StorageService {

    // Đọc đường dẫn từ file application.properties
    @Value("${app.storage.upload-dir}")
    private String uploadDir;

    private Path rootLocation;

    // Chạy ngay khi Backend vừa khởi động — chỉ dùng app.storage.upload-dir (không fallback)
    @PostConstruct
    public void init() {
        try {
            rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(rootLocation);
            verifyWritable(rootLocation);
            uploadDir = rootLocation.toString();
            System.out.println("=== Thư mục lưu trữ file (duy nhất): " + rootLocation + " ===");
        } catch (Exception e) {
            String hint = "Mở CMD (Run as Administrator) và chạy: icacls \""
                    + uploadDir + "\" /grant Users:(OI)(CI)M /T";
            throw new RuntimeException(
                    "Không thể ghi vào thư mục lưu trữ " + uploadDir + ". " + hint, e);
        }
    }

    private void verifyWritable(Path dir) throws IOException {
        Path testFile = Files.createTempFile(dir, ".write-test-", ".tmp");
        try {
            Files.writeString(testFile, "ok");
        } finally {
            Files.deleteIfExists(testFile);
        }
        // Trên Windows, thử set quyền ghi nếu thư mục vừa tạo
        if (!Files.isWritable(dir)) {
            throw new IOException("Thư mục không có quyền ghi: " + dir);
        }
    }

    public String getUploadDir() {
        return uploadDir;
    }

    // Hàm lưu file (Đã đổi tên file thành UUID để chống trùng lặp tên)
    public String storeFile(InputStream fileInputStream, String originalName) {
        try {
            if (originalName == null || originalName.isBlank()) {
                originalName = "upload.bin";
            }
            // Giữ lại phần đuôi mở rộng của file (vd: .pdf, .png)
            String extension = "";
            int i = originalName.lastIndexOf('.');
            if (i > 0) {
                extension = originalName.substring(i);
            }
            
            // Tạo tên file mới để tránh người dùng up 2 file trùng tên
            String savedFileName = UUID.randomUUID().toString() + extension;
            Path destinationFile = rootLocation.resolve(savedFileName).normalize();
            if (!destinationFile.startsWith(rootLocation)) {
                throw new SecurityException("Đường dẫn file không hợp lệ");
            }

            Files.copy(fileInputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            return savedFileName;
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lưu file vật lý: " + e.getMessage(), e);
        }
    }

    // Hàm lấy dung lượng file
    public long getFileSize(String savedFileName) {
        try {
            Path file = rootLocation.resolve(savedFileName);
            return Files.size(file);
        } catch (Exception e) {
            return 0;
        }
    }

    // Hàm xóa file vật lý
    public void deletePhysicalFile(String savedFileName) {
        try {
            Path file = rootLocation.resolve(savedFileName);
            Files.deleteIfExists(file);
        } catch (Exception e) {
            throw new RuntimeException("Không thể xóa file vật lý: " + e.getMessage(), e);
        }
    }

    // Hàm tạo ZIP của file vật lý
    public void zipFiles(java.util.List<String> filePathList, OutputStream outputStream) {
        try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
            for (String filePath : filePathList) {
                Path file = rootLocation.resolve(filePath);
                if (Files.exists(file) && Files.isRegularFile(file)) {
                    String entryName = file.getFileName().toString();
                    ZipEntry entry = new ZipEntry(entryName);
                    zipOut.putNextEntry(entry);
                    
                    try (InputStream fileInput = Files.newInputStream(file)) {
                        fileInput.transferTo(zipOut);
                    }
                    
                    zipOut.closeEntry();
                }
            }
            zipOut.finish();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo ZIP: " + e.getMessage(), e);
        }
    }

    // Hàm tạo ZIP với cấu trúc folder (recursive)
    public void zipFolderStructure(java.util.List<com.filemanager.entity.FileMetadata> files, 
                                  String baseFolderName, OutputStream outputStream) {
        try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
            for (com.filemanager.entity.FileMetadata file : files) {
                if (!Boolean.TRUE.equals(file.getIsFolder()) && !Boolean.TRUE.equals(file.getIsDeleted())) {
                    Path filePath = rootLocation.resolve(file.getFilePath());
                    if (Files.exists(filePath)) {
                        // Tạo entry path dưới thư mục cơ sở
                        String entryName = baseFolderName + "/" + file.getFileName();
                        ZipEntry entry = new ZipEntry(entryName);
                        zipOut.putNextEntry(entry);
                        
                        try (InputStream fileInput = Files.newInputStream(filePath)) {
                            fileInput.transferTo(zipOut);
                        }
                        
                        zipOut.closeEntry();
                    }
                }
            }
            zipOut.finish();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo ZIP folder: " + e.getMessage(), e);
        }
    }
}