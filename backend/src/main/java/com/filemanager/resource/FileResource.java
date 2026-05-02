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
import java.util.List;
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

   @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadFile(
            @FormDataParam("file") InputStream fileInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail,
            @FormDataParam("parentId") Long parentId,
            @Context ContainerRequestContext requestContext) {
        
        try {
            Object userIdObj = requestContext.getProperty("userId");
            if (userIdObj == null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Cần đăng nhập").build();
            }
            Long userId = ((Number) userIdObj).longValue();
            String originalName = fileDetail.getFileName();
            
            // 1. Lưu file xuống disk (Lưu tạm để lấy chính xác dung lượng byte)
            String savedFileName = storageService.storeFile(fileInputStream, originalName);
            long exactSizeBytes = storageService.getFileSize(savedFileName);
            
            // 2. Kiểm tra dung lượng Quota TRƯỚC KHI lưu vào Database
            try {
                quotaService.validateAndAddQuota(userId, exactSizeBytes);
            } catch (WebApplicationException e) {
                // Nếu vượt dung lượng -> Xóa file vật lý vừa lưu tạm và ném lỗi ra
                Files.deleteIfExists(Paths.get(storageService.getUploadDir()).resolve(savedFileName));
                throw e;
            }
            
            // 3. Nếu hợp lệ, lưu metadata vào Database
            FileMetadata metadata = new FileMetadata();
            metadata.setOwnerId(userId);
            metadata.setFileName(originalName);
            metadata.setFilePath(savedFileName);
            metadata.setFileSize(exactSizeBytes);
            metadata.setMimeType(Files.probeContentType(Paths.get(storageService.getUploadDir()).resolve(savedFileName)));
            metadata.setIsFolder(false); // Xác nhận đây là file, không phải thư mục
            metadata.setParentId(parentId);
            
            fileRepository.save(metadata);
            
            return Response.ok(metadata).build();
        } catch (WebApplicationException we) {
            throw we; // Ném tiếp lỗi Quota ra ngoài
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("Lỗi upload: " + e.getMessage()).build();
        }
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
    @Path("/list/all") // Đổi từ "/all" thành "/list/all"
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
