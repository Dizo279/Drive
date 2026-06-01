package com.filemanager.resource;

import com.filemanager.entity.FileMetadata;
import com.filemanager.entity.FileShare;
import com.filemanager.entity.Notification;
import com.filemanager.entity.User;
import com.filemanager.repository.FileRepository;
import com.filemanager.repository.FileShareRepository;
import com.filemanager.repository.UserRepository;
import com.filemanager.service.NotificationService;
import com.filemanager.service.QuotaService;
import com.filemanager.service.StorageService;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;

@Component
@Path("/files")
public class FileResource {

    public static class ShareRequest {
        public java.util.List<String> emails;
        public Integer expireDays; // Số ngày hết hạn
    }

    public static class SharedItemDTO {
        private Long shareId;
        private Long fileId;
        private String fileName;
        private String targetEmail;
        private LocalDateTime expiresAt;
        private String shareToken;

        public Long getShareId() { return shareId; }
        public void setShareId(Long shareId) { this.shareId = shareId; }
        
        public Long getFileId() { return fileId; }
        public void setFileId(Long fileId) { this.fileId = fileId; }
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public String getTargetEmail() { return targetEmail; }
        public void setTargetEmail(String targetEmail) { this.targetEmail = targetEmail; }
        
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
        
        public String getShareToken() { return shareToken; }
        public void setShareToken(String shareToken) { this.shareToken = shareToken; }
    }

    @Inject
    private StorageService storageService;

    @Inject
    private FileRepository fileRepository;

    @Value("${app.storage.upload-dir}")
    private String uploadDir;

    @Inject
    private QuotaService quotaService;

    @Inject
    UserRepository userRepository;

    @Inject
    FileShareRepository fileShareRepository;

    @Inject
    private NotificationService notificationService;
    // API UPLOAD FILE MỚI: Lưu file xuống disk, kiểm tra quota, sau đó lưu metadata vào Database
   @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadFile(
            @FormDataParam("file") InputStream fileInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail,
            @FormDataParam("parentId") Long parentIdForm,
            @QueryParam("parentId") Long parentIdQuery,
            @Context ContainerRequestContext requestContext) {

        try {
            Object userIdObj = requestContext.getProperty("userId");
            if (userIdObj == null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Cần đăng nhập").build();
            }
            if (fileInputStream == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Thiếu dữ liệu file\"}").build();
            }

            Long userId = ((Number) userIdObj).longValue();
            Long parentId = parentIdForm != null ? parentIdForm : parentIdQuery;
            String originalName = resolveUploadFileName(fileDetail);

            // 1. Lưu file xuống disk
            String savedFileName = storageService.storeFile(fileInputStream, originalName);
            long exactSizeBytes = storageService.getFileSize(savedFileName);

            // 2. Kiểm tra quota
            try {
                quotaService.validateAndAddQuota(userId, exactSizeBytes);
            } catch (WebApplicationException e) {
                Files.deleteIfExists(Paths.get(storageService.getUploadDir()).resolve(savedFileName));
                throw e;
            }

            // 3. Lưu metadata
            java.nio.file.Path savedPath = Paths.get(storageService.getUploadDir()).resolve(savedFileName);
            String mimeType = Files.probeContentType(savedPath);
            if (mimeType == null || mimeType.isBlank()) {
                mimeType = guessMimeType(originalName);
            }

            FileMetadata metadata = new FileMetadata();
            metadata.setOwnerId(userId);
            metadata.setFileName(originalName);
            metadata.setFilePath(savedFileName);
            metadata.setFileSize(exactSizeBytes);
            metadata.setMimeType(mimeType);
            metadata.setIsFolder(false);
            metadata.setParentId(parentId);

            fileRepository.save(metadata);

            return Response.ok(metadata).build();
        } catch (WebApplicationException we) {
            throw we;
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError()
                    .entity("{\"error\": \"Lỗi upload: " + e.getMessage() + "\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /** Lấy tên file từ multipart (Android/iOS đôi khi không gửi disposition đầy đủ). */
    private String resolveUploadFileName(FormDataContentDisposition fileDetail) {
        if (fileDetail != null && fileDetail.getFileName() != null && !fileDetail.getFileName().isBlank()) {
            String name = fileDetail.getFileName().trim();
            int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
            if (slash >= 0 && slash < name.length() - 1) {
                name = name.substring(slash + 1);
            }
            return name;
        }
        return "upload_" + UUID.randomUUID().toString().substring(0, 8) + ".bin";
    }

    private String guessMimeType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (lower.endsWith(".zip")) return "application/zip";
        return "application/octet-stream";
    }

    /**
     * API UPLOAD CẢ FOLDER: Nhận nhiều file kèm đường dẫn tương đối (relative path),
     * tự động tạo cấu trúc thư mục đệ quy trên DB rồi lưu file vật lý.
     *
     * Form fields:
     *   - files[]: danh sách InputStream của từng file
     *   - fileDetails[]: FormDataContentDisposition tương ứng (chứa tên file)
     *   - relativePaths[]: đường dẫn tương đối của từng file trong folder gốc
     *                      ví dụ: "MyFolder/sub/image.png"
     *   - parentId (optional): ID thư mục cha nơi đặt folder tải lên
     */
    @POST
    @Path("/upload-folder")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadFolder(
            @FormDataParam("files") List<org.glassfish.jersey.media.multipart.FormDataBodyPart> fileParts,
            @FormDataParam("relativePaths") List<org.glassfish.jersey.media.multipart.FormDataBodyPart> pathParts,
            @FormDataParam("parentId") Long parentId,
            @Context ContainerRequestContext requestContext) {

        try {
            Object userIdObj = requestContext.getProperty("userId");
            if (userIdObj == null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\": \"Cần đăng nhập\"}").build();
            }
            Long userId = ((Number) userIdObj).longValue();

            if (fileParts == null || fileParts.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Không có file nào được gửi lên\"}").build();
            }

            // Map: đường dẫn thư mục -> ID thư mục đã tạo (cache để tránh tạo trùng)
            Map<String, Long> folderCache = new HashMap<>();

            int successCount = 0;
            long totalBytes = 0;
            List<String> errors = new ArrayList<>();

            for (int i = 0; i < fileParts.size(); i++) {
                org.glassfish.jersey.media.multipart.FormDataBodyPart filePart = fileParts.get(i);

                // Lấy đường dẫn tương đối tương ứng
                String relativePath = "";
                if (pathParts != null && i < pathParts.size()) {
                    relativePath = pathParts.get(i).getValue();
                }

                try {
                    InputStream fileInputStream = filePart.getValueAs(InputStream.class);
                    FormDataContentDisposition fileDetail = filePart.getFormDataContentDisposition();
                    String originalName = fileDetail.getFileName();
                    if (originalName == null || originalName.isBlank()) {
                        originalName = "unknown_file";
                    }

                    // 1. Lưu file vật lý xuống disk
                    String savedFileName = storageService.storeFile(fileInputStream, originalName);
                    long exactSizeBytes = storageService.getFileSize(savedFileName);

                    // 2. Kiểm tra quota
                    try {
                        quotaService.validateAndAddQuota(userId, exactSizeBytes);
                    } catch (WebApplicationException e) {
                        Files.deleteIfExists(Paths.get(storageService.getUploadDir()).resolve(savedFileName));
                        errors.add("Vượt quota khi tải: " + relativePath);
                        continue;
                    }

                    // 3. Tạo cấu trúc thư mục đệ quy theo relativePath
                    // relativePath ví dụ: "MyProject/src/main/App.java"
                    // -> tạo folder MyProject (cha: parentId), src (cha: MyProject), main (cha: src)
                    // -> lưu file App.java vào folder main
                    Long fileParentId = resolveOrCreateFolderPath(relativePath, userId, parentId, folderCache);

                    // 4. Lưu metadata file vào DB
                    FileMetadata metadata = new FileMetadata();
                    metadata.setOwnerId(userId);
                    metadata.setFileName(originalName);
                    metadata.setFilePath(savedFileName);
                    metadata.setFileSize(exactSizeBytes);
                    metadata.setMimeType(Files.probeContentType(
                            Paths.get(storageService.getUploadDir()).resolve(savedFileName)));
                    metadata.setIsFolder(false);
                    metadata.setParentId(fileParentId);

                    fileRepository.save(metadata);
                    successCount++;
                    totalBytes += exactSizeBytes;

                } catch (Exception ex) {
                    ex.printStackTrace();
                    errors.add("Lỗi file: " + relativePath + " - " + ex.getMessage());
                }
            }

            String msg = String.format(
                    "{\"message\": \"Tải lên hoàn tất\", \"successCount\": %d, \"totalBytes\": %d, \"errors\": %d}",
                    successCount, totalBytes, errors.size());
            return Response.ok(msg).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * Phân tích đường dẫn tương đối, tạo (hoặc lấy từ cache) từng thư mục cha theo cấu trúc.
     * Trả về ID của thư mục chứa file (thư mục lá cuối cùng trước tên file).
     *
     * Ví dụ: relativePath = "MyProject/src/App.java", parentId = null
     *  -> tạo "MyProject" (root), "src" (cha = MyProject)
     *  -> trả về ID của "src"
     */
    private Long resolveOrCreateFolderPath(String relativePath, Long userId, Long rootParentId,
                                            Map<String, Long> folderCache) {
        if (relativePath == null || relativePath.isBlank()) {
            return rootParentId;
        }

        // Chuẩn hóa dấu phân cách (Windows dùng \, nhưng browser gửi /)
        String normalized = relativePath.replace("\\", "/");

        // Tách thành các phần: phần cuối là tên file, các phần trước là thư mục
        String[] parts = normalized.split("/");

        // Chỉ có tên file, không có thư mục cha
        if (parts.length <= 1) {
            return rootParentId;
        }

        // Duyệt từng cấp thư mục (bỏ phần tử cuối cùng là tên file)
        Long currentParentId = rootParentId;
        StringBuilder pathKey = new StringBuilder();

        for (int i = 0; i < parts.length - 1; i++) {
            String folderName = parts[i];
            if (folderName.isBlank()) continue;

            if (pathKey.length() > 0) pathKey.append("/");
            pathKey.append(folderName);

            String cacheKey = userId + ":" + (rootParentId != null ? rootParentId : "null") + ":" + pathKey;

            if (folderCache.containsKey(cacheKey)) {
                currentParentId = folderCache.get(cacheKey);
            } else {
                // Tạo thư mục mới trong DB
                FileMetadata folder = new FileMetadata();
                folder.setOwnerId(userId);
                folder.setFileName(folderName);
                folder.setIsFolder(true);
                folder.setParentId(currentParentId);
                folder.setFileSize(0L);
                folder.setFilePath("");
                fileRepository.save(folder);

                currentParentId = folder.getId();
                folderCache.put(cacheKey, currentParentId);
            }
        }

        return currentParentId;
    }

    // CẬP NHẬT API LẤY DANH SÁCH FILE THEO THƯ MỤC
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMyFiles(@QueryParam("parentId") Long parentId, @Context ContainerRequestContext requestContext) {
        Object userIdObj = requestContext.getProperty("userId");
        if (userIdObj == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\": \"Cần đăng nhập\"}").build();
        }
        
        Long userId = ((Number) userIdObj).longValue();
        List<FileMetadata> files;

        // Nếu client truyền parentId, lấy các file trong thư mục đó. Nếu không, lấy ở thư mục gốc.
        if (parentId != null) {
            files = fileRepository.findByOwnerIdAndParentIdAndIsDeletedFalse(userId, parentId);
        } else {
            files = fileRepository.findByOwnerIdAndParentIdIsNullAndIsDeletedFalse(userId);
        }
        
        return Response.ok(files).build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFileDetail(@PathParam("id") Long id, @Context ContainerRequestContext requestContext) {
        if (id == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Thiếu id").build();
        }
        Object userIdObj = requestContext.getProperty("userId");
        if (userIdObj == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        
        Long userId = ((Number) userIdObj).longValue();
        Optional<FileMetadata> fileOpt = fileRepository.findById(id);
        
        // Chỉ trả về file nếu file đó tồn tại VÀ thuộc về user đang request
        if (fileOpt.isPresent()
                && fileOpt.get().getOwnerId().equals(userId)
                && !Boolean.TRUE.equals(fileOpt.get().getIsDeleted())) {
            return Response.ok(fileOpt.get()).build();
        }
        
        return Response.status(Response.Status.NOT_FOUND).entity("File không tồn tại hoặc bạn không có quyền xem").build();
    }

    @GET
    @Path("/{id}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadFile(@PathParam("id") Long id, @Context ContainerRequestContext requestContext) {
        if (id == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Thiếu id").build();
        }
        Object userIdObj = requestContext.getProperty("userId");
        if (userIdObj == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        
        Long userId = ((Number) userIdObj).longValue();
        Optional<FileMetadata> fileOpt = fileRepository.findById(id);
        
        // Kiểm tra quyền sở hữu
        if (fileOpt.isEmpty()
                || !fileOpt.get().getOwnerId().equals(userId)
                || Boolean.TRUE.equals(fileOpt.get().getIsDeleted())) {
            return Response.status(Response.Status.NOT_FOUND).entity("File không tồn tại hoặc bạn không có quyền tải").build();
        }

        FileMetadata metadata = fileOpt.get();
        java.nio.file.Path filePath = Paths.get(storageService.getUploadDir()).resolve(metadata.getFilePath());

        if (!Files.exists(filePath)) {
            return Response.status(Response.Status.NOT_FOUND).entity("Không tìm thấy file vật lý trên server").build();
        }

        // Tạo luồng đọc file
        StreamingOutput fileStream = new StreamingOutput() {
            @Override
            public void write(OutputStream output) {
                try (InputStream input = Files.newInputStream(filePath)) {
                    input.transferTo(output);
                } catch (Exception e) {
                    throw new WebApplicationException("Lỗi đọc file", e);
                }
            }
        };

        // Báo cho trình duyệt biết đây là file đính kèm và tên file là gì
        return Response.ok(fileStream)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getFileName() + "\"")
                .build();
    }

    // API DOWNLOAD FOLDER (nén toàn bộ folder thành ZIP)
    @GET
    @Path("/{id}/download-folder")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadFolder(@PathParam("id") Long id, @Context ContainerRequestContext requestContext) {
        if (id == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Thiếu id").build();
        }
        Object userIdObj = requestContext.getProperty("userId");
        if (userIdObj == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        
        Long userId = ((Number) userIdObj).longValue();
        Optional<FileMetadata> fileOpt = fileRepository.findById(id);
        
        // Kiểm tra quyền sở hữu và phải là folder
        if (fileOpt.isEmpty()
                || !fileOpt.get().getOwnerId().equals(userId)
                || Boolean.TRUE.equals(fileOpt.get().getIsDeleted())
                || !Boolean.TRUE.equals(fileOpt.get().getIsFolder())) {
            return Response.status(Response.Status.NOT_FOUND).entity("Thư mục không tồn tại hoặc bạn không có quyền tải").build();
        }

        FileMetadata folderMetadata = fileOpt.get();
        
        // Lấy tất cả file/folder bên trong folder này (đệ quy)
        List<FileMetadata> allFilesInFolder = fileRepository.findAllRecursiveInFolder(id);
        
        // Tạo luồng ZIP trực tiếp
        StreamingOutput fileStream = new StreamingOutput() {
            @Override
            public void write(OutputStream output) {
                try {
                    storageService.zipFolderStructure(allFilesInFolder, folderMetadata.getFileName(), output);
                } catch (Exception e) {
                    throw new WebApplicationException("Lỗi tạo ZIP file", e);
                }
            }
        };

        // Tên file ZIP sẽ là tên folder + .zip
        String zipFileName = folderMetadata.getFileName() + ".zip";
        
        return Response.ok(fileStream)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFileName + "\"")
                .build();
    }

    // SOFT DELETE: chuyển vào Trash (khôi phục trong 30 ngày)
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteFile(@PathParam("id") Long id, @Context ContainerRequestContext requestContext) {
        try {
            if (id == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Thiếu id\"}").build();
            }
            Object userIdObj = requestContext.getProperty("userId");
            if (userIdObj == null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\": \"Cần đăng nhập\"}").build();
            }
            Long userId = ((Number) userIdObj).longValue();

            FileMetadata target = fileRepository.findById(id).orElse(null);
            if (target == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            // Chỉ chủ sở hữu mới được chuyển vào Trash
            if (target.getOwnerId() == null || !target.getOwnerId().equals(userId)) {
                return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"Bạn không có quyền xóa mục này\"}").build();
            }

            // Nếu đã ở Trash thì không làm gì thêm
            if (Boolean.TRUE.equals(target.getIsDeleted())) {
                return Response.ok("{\"message\": \"Mục này đã nằm trong Trash\"}").build();
            }

            moveToTrashRecursively(target, java.time.LocalDateTime.now());

            return Response.ok("{\"message\": \"Đã chuyển vào Trash\"}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    private void moveToTrashRecursively(FileMetadata item, java.time.LocalDateTime deletedAt) {
        item.setIsDeleted(true);
        item.setDeletedAt(deletedAt);
        fileRepository.save(item);

        if (Boolean.TRUE.equals(item.getIsFolder())) {
            List<FileMetadata> children = fileRepository.findByParentId(item.getId());
            for (FileMetadata child : children) {
                if (!Boolean.TRUE.equals(child.getIsDeleted())) {
                    moveToTrashRecursively(child, deletedAt);
                }
            }
        }
    }

    // Ẩn list Trash của user hiện tại, giờ chỉ hiển thị root items
    @GET
    @Path("/trash")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTrash(@Context ContainerRequestContext requestContext) {
        Object userIdObj = requestContext.getProperty("userId");
        if (userIdObj == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\": \"Cần đăng nhập\"}").build();
        }
        Long userId = ((Number) userIdObj).longValue();
        return Response.ok(fileRepository.findRootTrashItemsByOwner(userId)).build();
    }

    // Khôi phục file/folder từ Trash
    @POST
    @Path("/{id}/restore")
    @Produces(MediaType.APPLICATION_JSON)
    public Response restoreFromTrash(@PathParam("id") Long id, @Context ContainerRequestContext requestContext) {
        try {
            if (id == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Thiếu id\"}").build();
            }
            Object userIdObj = requestContext.getProperty("userId");
            if (userIdObj == null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\": \"Cần đăng nhập\"}").build();
            }
            Long userId = ((Number) userIdObj).longValue();

            FileMetadata target = fileRepository.findById(id).orElse(null);
            if (target == null || !target.getOwnerId().equals(userId)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            if (!Boolean.TRUE.equals(target.getIsDeleted())) {
                return Response.ok("{\"message\": \"Mục này không nằm trong Trash\"}").build();
            }

            restoreRecursively(target);
            return Response.ok("{\"message\": \"Đã khôi phục\"}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    private void restoreRecursively(FileMetadata item) {
        // Nếu parent đang ở Trash (hoặc đã bị xóa), đưa về root để tránh “mồ côi”
        Long parentId = item.getParentId();
        if (parentId != null) {
            FileMetadata parent = fileRepository.findById(parentId).orElse(null);
            if (parent == null || Boolean.TRUE.equals(parent.getIsDeleted())) {
                item.setParentId(null);
            }
        }

        item.setIsDeleted(false);
        item.setDeletedAt(null);
        fileRepository.save(item);

        if (Boolean.TRUE.equals(item.getIsFolder())) {
            List<FileMetadata> children = fileRepository.findByParentId(item.getId());
            for (FileMetadata child : children) {
                if (Boolean.TRUE.equals(child.getIsDeleted())) {
                    restoreRecursively(child);
                }
            }
        }
    }

    // Xóa vĩnh viễn 1 mục trong Trash
    @DELETE
    @Path("/{id}/permanent")
    @Produces(MediaType.APPLICATION_JSON)
    public Response permanentlyDelete(@PathParam("id") Long id, @Context ContainerRequestContext requestContext) {
        try {
            if (id == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Thiếu id\"}").build();
            }
            Object userIdObj = requestContext.getProperty("userId");
            if (userIdObj == null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\": \"Cần đăng nhập\"}").build();
            }
            Long userId = ((Number) userIdObj).longValue();

            FileMetadata target = fileRepository.findById(id).orElse(null);
            if (target == null || !userId.equals(target.getOwnerId())) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            if (!Boolean.TRUE.equals(target.getIsDeleted())) {
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Mục này chưa nằm trong Trash\"}").build();
            }

            hardDeleteRecursively(target);
            return Response.ok("{\"message\": \"Đã xóa vĩnh viễn\"}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    // Dọn sạch Trash của user hiện tại
    @DELETE
    @Path("/trash/empty")
    @Produces(MediaType.APPLICATION_JSON)
    public Response emptyTrash(@Context ContainerRequestContext requestContext) {
        try {
            Object userIdObj = requestContext.getProperty("userId");
            if (userIdObj == null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\": \"Cần đăng nhập\"}").build();
            }
            Long userId = ((Number) userIdObj).longValue();

            // Chỉ lấy các item gốc để xóa đệ quy, tránh xóa trùng lặp các item con
            List<FileMetadata> trashItems = fileRepository.findRootTrashItemsByOwner(userId);
            int deletedCount = 0;
            for (FileMetadata item : trashItems) {
                if (item != null && item.getId() != null) {
                    hardDeleteRecursively(item);
                    deletedCount++;
                }
            }
            return Response.ok("{\"message\": \"Đã dọn Trash\", \"deletedCount\": " + deletedCount + "}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    private void hardDeleteRecursively(FileMetadata item) {
        if (item == null || item.getId() == null) {
            return;
        }

        if (Boolean.TRUE.equals(item.getIsFolder())) {
            List<FileMetadata> children = fileRepository.findByParentId(item.getId());
            for (FileMetadata child : children) {
                hardDeleteRecursively(child);
            }
        } else {
            // Xóa file vật lý trên disk
            try {
                if (item.getFilePath() != null && !item.getFilePath().isBlank()) {
                    java.nio.file.Path physicalPath = java.nio.file.Paths.get(uploadDir, item.getFilePath());
                    java.nio.file.Files.deleteIfExists(physicalPath);
                }
            } catch (Exception e) {
                System.err.println("Không thể xóa file vật lý: " + item.getFilePath());
            }
            // Không cần cập nhật user.usedQuota ở đây nữa.
            // Quota được tính chính xác theo thời gian thực thông qua API /quota
            // sử dụng SUM query trực tiếp trên bảng files, tránh race condition.
        }

        // Xóa tất cả share liên quan
        if (item.getId() != null) {
            List<FileShare> shares = fileShareRepository.findByFileId(item.getId());
            if (!shares.isEmpty()) {
                fileShareRepository.deleteAll(shares);
            }
        }

        fileRepository.delete(item);
    }

    // ĐỔI TÊN FILE/FOLDER
    @PUT
    @Path("/{id}/rename")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response renameFile(@PathParam("id") Long id, Map<String, String> body, @Context ContainerRequestContext requestContext) {
        try {
            if (id == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Thiếu id\"}").build();
            }
            Object userIdObj = requestContext.getProperty("userId");
            if (userIdObj == null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\": \"Cần đăng nhập\"}").build();
            }
            Long userId = ((Number) userIdObj).longValue();
            
            String newName = body.get("name");
            if (newName == null || newName.trim().isEmpty()) {
                 return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Tên mới không hợp lệ\"}").build();
            }

            FileMetadata file = fileRepository.findById(id).orElse(null);
            if (file == null || !file.getOwnerId().equals(userId)) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"File không tồn tại\"}").build();
            }
            
            file.setFileName(newName.trim());
            fileRepository.save(file);
            return Response.ok(file).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }


   // 1. API TẠO LINK CHIA SẺ
    @POST
    @Path("/{id}/share")
    @Consumes(MediaType.APPLICATION_JSON) // Bổ sung Consumes để nhận JSON Body
    @Produces(MediaType.APPLICATION_JSON)
    public Response createShareLink(@PathParam("id") Long fileId, ShareRequest requestData, @Context ContainerRequestContext requestContext) {
        try {
            if (fileId == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Thiếu id\"}").build();
            }
            Object userIdObj = requestContext.getProperty("userId");
            if (userIdObj == null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\": \"Cần đăng nhập\"}").build();
            }
            Long userId = ((Number) userIdObj).longValue();

            FileMetadata file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy file"));

            if (file.getOwnerId() == null || !file.getOwnerId().equals(userId)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("{\"error\": \"Bạn không có quyền chia sẻ file này\"}").build();
            }
            if (Boolean.TRUE.equals(file.getIsDeleted())) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"File đang nằm trong Trash, không thể chia sẻ\"}").build();
            }

            // LUỒNG 1: CHIA SẺ PUBLIC LINK (Nếu không nhập email nào)
            if (requestData == null || requestData.emails == null || requestData.emails.isEmpty()) {
                FileShare existingPublic = fileShareRepository.findByFileIdAndSharedWithIsNull(fileId).orElse(null);
                if (existingPublic != null) {
                    return Response.ok("{\"shareToken\": \"" + existingPublic.getShareToken() + "\"}").build();
                }

                FileShare newShare = new FileShare();
                newShare.setFileId(fileId);
                newShare.setShareToken(UUID.randomUUID().toString());
                newShare.setCreatedAt(LocalDateTime.now());
                newShare.setSharedBy(userId);
                if (requestData != null && requestData.expireDays != null) {
                    newShare.setExpiresAt(LocalDateTime.now().plusDays(requestData.expireDays));
                }
                fileShareRepository.save(newShare);
                return Response.ok("{\"shareToken\": \"" + newShare.getShareToken() + "\"}").build();
            }

            // LUỒNG 2: CHIA SẺ ĐỊNH DANH QUA EMAIL
            int successCount = 0;
            User currentUser = userRepository.findById(userId).orElse(null);
            for (String email : requestData.emails) {
                // Tìm ID người nhận dựa trên Email
                User targetUser = userRepository.findByEmail(email.trim()).orElse(null);
                if (targetUser == null || targetUser.getId().equals(userId)) {
                    continue; // Bỏ qua nếu email không tồn tại hoặc tự share cho chính mình
                }

                // Kiểm tra xem đã share cho người này trước đó chưa
                FileShare existing = fileShareRepository.findByFileIdAndSharedWith(fileId, targetUser.getId()).orElse(null);
                if (existing == null) {
                    FileShare newShare = new FileShare();
                    newShare.setFileId(fileId);
                    newShare.setShareToken(UUID.randomUUID().toString());
                    newShare.setCreatedAt(LocalDateTime.now());
                    newShare.setSharedBy(userId);
                    newShare.setSharedWith(targetUser.getId()); // Gắn định danh người nhận
                    
                    if (requestData.expireDays != null) {
                        newShare.setExpiresAt(LocalDateTime.now().plusDays(requestData.expireDays));
                    }
                    fileShareRepository.save(newShare);

                    // Tạo notification cho người nhận
                    Notification notification = new Notification();
                    notification.setUserId(targetUser.getId());
                    notification.setType("FILE_SHARED");
                    notification.setMessage("Người dùng " + (currentUser != null ? currentUser.getUsername() : "Ai đó") + " đã chia sẻ file \"" + file.getFileName() + "\" với bạn.");
                    notification.setTargetUrl("/shared");
                    notificationService.sendNotification(notification);
                }
                successCount++;
            }

            return Response.ok("{\"message\": \"Đã chia sẻ thành công cho " + successCount + " người\"}").build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    @GET
    @Path("/shared/{token}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadSharedFile(@PathParam("token") String token) {
        System.out.println("\n========== BẮT ĐẦU TẢI FILE ==========");
        try {
            FileShare share = fileShareRepository.findByShareToken(token)
                    .orElseThrow(() -> new RuntimeException("Liên kết không hợp lệ hoặc đã hết hạn"));

            if (share.getExpiresAt() != null && share.getExpiresAt().isBefore(LocalDateTime.now())) {
                return Response.status(Response.Status.GONE).entity("Liên kết đã hết hạn").build();
            }

            Long fileId = share.getFileId();
            if (fileId == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Tệp tin không tồn tại").build();
            }
            FileMetadata metadata = fileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("Tệp tin không tồn tại"));
            if (Boolean.TRUE.equals(metadata.getIsDeleted())) {
                return Response.status(Response.Status.GONE).entity("Tệp tin đã bị xóa (đang nằm trong Trash)").build();
            }

            // SỬ DỤNG BIẾN uploadDir ĐÃ CẤU HÌNH TỪ application.properties
            java.nio.file.Path path = java.nio.file.Paths.get(uploadDir, metadata.getFilePath());
            
            System.out.println("-> Kiểm tra đường dẫn: " + path.toAbsolutePath());

            if (!java.nio.file.Files.exists(path)) {
                System.out.println("-> LỖI: Không thấy file tại " + path.toAbsolutePath());
                return Response.status(Response.Status.NOT_FOUND).entity("File vật lý không tồn tại").build();
            }

            System.out.println("-> File OK! Đang chuẩn bị gửi dữ liệu...");

            StreamingOutput fileStream = output -> {
                java.nio.file.Files.copy(path, output);
                output.flush();
            };

            // Mã hóa tên file để tránh lỗi "Unsafe" trên Chrome khi có dấu tiếng Việt
            String safeFileName = java.net.URLEncoder.encode(metadata.getFileName(), "UTF-8").replace("+", "%20");

            System.out.println("========== HOÀN TẤT TẢI FILE ==========\n");

            return Response.ok(fileStream)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + safeFileName)
                    .header("Content-Length", metadata.getFileSize())
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    // API TẠO THƯ MỤC MỚI
    @POST
    @Path("/folder")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createFolder(FileMetadata requestData, @Context ContainerRequestContext requestContext) {
        try {
            Object userIdObj = requestContext.getProperty("userId");
            if (userIdObj == null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\": \"Cần đăng nhập\"}").build();
            }
            Long userId = ((Number) userIdObj).longValue();

            // Kiểm tra tên thư mục
            if (requestData.getFileName() == null || requestData.getFileName().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Tên thư mục không được để trống\"}").build();
            }

            FileMetadata newFolder = new FileMetadata();
            newFolder.setOwnerId(userId);
            newFolder.setFileName(requestData.getFileName());
            newFolder.setIsFolder(true); // Đánh dấu đây là thư mục
            newFolder.setParentId(requestData.getParentId()); // Thư mục này nằm trong thư mục nào?
            newFolder.setFileSize(0L);
            newFolder.setFilePath("");
            
            fileRepository.save(newFolder);
            
            return Response.ok(newFolder).build();
            
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    @GET
    @Path("/list/all") // "/list/all"
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllFilesOfUser(@Context ContainerRequestContext requestContext) {
        try {
            // Ép kiểu an toàn hơn đề phòng userId bị null
            Object userIdObj = requestContext.getProperty("userId");
            if (userIdObj == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
            Long userId = Long.valueOf(userIdObj.toString());
            
            List<FileMetadata> allFiles = fileRepository.findByOwnerIdAndIsDeletedFalse(userId);
            return Response.ok(allFiles).build();
            
        } catch (Exception e) {
            e.printStackTrace(); // In chi tiết lỗi ra màn hình Terminal của Backend
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/list/shared-by-me")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFilesSharedByMe(@Context ContainerRequestContext requestContext) {
        try {
            // KIỂM TRA NULL AN TOÀN TRƯỚC KHI ÉP KIỂU
            Object userIdObj = requestContext.getProperty("userId");
            if (userIdObj == null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\": \"Phiên đăng nhập hết hạn hoặc không hợp lệ\"}").build();
            }
            Long userId = ((Number) userIdObj).longValue();
            
            List<FileShare> shares = fileShareRepository.findBySharedBy(userId);
            List<SharedItemDTO> result = new java.util.ArrayList<>();

            for (FileShare s : shares) {
                SharedItemDTO dto = new SharedItemDTO();
                dto.setShareId(s.getId());
                dto.setFileId(s.getFileId());
                dto.setExpiresAt(s.getExpiresAt());
                dto.setShareToken(s.getShareToken());

                Long fileId = s.getFileId();
                if (fileId != null) {
                    fileRepository.findById(fileId).ifPresent(f -> {
                        if (!Boolean.TRUE.equals(f.getIsDeleted())) {
                            dto.setFileName(f.getFileName());
                        }
                    });
                }

                Long sharedWith = s.getSharedWith();
                if (sharedWith != null) {
                    userRepository.findById(sharedWith).ifPresent(u -> dto.setTargetEmail(u.getEmail()));
                } else {
                    dto.setTargetEmail("Public Link (Bất kỳ ai)");
                }
                result.add(dto);
            }
            return Response.ok(result).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

   // 2. API: Lấy danh sách các file NGƯỜI KHÁC chia sẻ cho mình
    @GET
    @Path("/list/shared-with-me")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFilesSharedWithMe(@Context ContainerRequestContext requestContext) {
        try {
            // KIỂM TRA NULL AN TOÀN TRƯỚC KHI ÉP KIỂU
            Object userIdObj = requestContext.getProperty("userId");
            if (userIdObj == null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\": \"Phiên đăng nhập hết hạn hoặc không hợp lệ\"}").build();
            }
            Long userId = ((Number) userIdObj).longValue();
            
            List<FileShare> shares = fileShareRepository.findBySharedWith(userId);
            List<SharedItemDTO> result = new java.util.ArrayList<>();

            for (FileShare s : shares) {
                SharedItemDTO dto = new SharedItemDTO();
                dto.setShareId(s.getId());
                dto.setFileId(s.getFileId());
                dto.setExpiresAt(s.getExpiresAt());
                dto.setShareToken(s.getShareToken());

                Long fileId = s.getFileId();
                if (fileId != null) {
                    fileRepository.findById(fileId).ifPresent(f -> {
                        if (!Boolean.TRUE.equals(f.getIsDeleted())) {
                            dto.setFileName(f.getFileName());
                        }
                    });
                }
                
                Long sharedBy = s.getSharedBy();
                if (sharedBy != null) {
                    userRepository.findById(sharedBy).ifPresent(u -> dto.setTargetEmail(u.getEmail()));
                }
                result.add(dto);
            }
            return Response.ok(result).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    @DELETE
    @Path("/revoke-share/{shareId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response revokeShare(@PathParam("shareId") Long shareId, @Context ContainerRequestContext requestContext) {
        try {
            if (shareId == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Thiếu shareId\"}").build();
            }
            Long userId = ((Number) requestContext.getProperty("userId")).longValue();
            FileShare share = fileShareRepository.findById(shareId).orElse(null);
            
            // Cả chủ sở hữu và người được chia sẻ đều có thể xóa/thu hồi
            if (share != null && (share.getSharedBy().equals(userId) || userId.equals(share.getSharedWith()))) {
                fileShareRepository.delete(share);
                return Response.ok("{\"message\": \"Đã thu hồi/xóa quyền truy cập\"}").build();
            }
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"Không có quyền thực hiện\"}").build();
        } catch (Exception e) { return Response.serverError().build(); }
    }
   
}
