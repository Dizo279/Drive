package com.filemanager.resource;

import com.filemanager.entity.FileMetadata;
import com.filemanager.entity.FileShare;
import com.filemanager.repository.FileRepository;
import com.filemanager.repository.FileShareRepository;
import com.filemanager.repository.UserRepository;
import com.filemanager.service.QuotaService;
import com.filemanager.service.StorageService;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.http.ResponseEntity;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.beans.factory.annotation.Value;

import jakarta.ws.rs.core.Response;
import com.filemanager.entity.FileMetadata;
import com.filemanager.entity.User;

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
            files = fileRepository.findByOwnerIdAndParentId(userId, parentId);
        } else {
            files = fileRepository.findByOwnerIdAndParentIdIsNull(userId);
        }
        
        return Response.ok(files).build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFileDetail(@PathParam("id") Long id, @Context ContainerRequestContext requestContext) {
        Object userIdObj = requestContext.getProperty("userId");
        if (userIdObj == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        
        Long userId = ((Number) userIdObj).longValue();
        Optional<FileMetadata> fileOpt = fileRepository.findById(id);
        
        // Chỉ trả về file nếu file đó tồn tại VÀ thuộc về user đang request
        if (fileOpt.isPresent() && fileOpt.get().getOwnerId().equals(userId)) {
            return Response.ok(fileOpt.get()).build();
        }
        
        return Response.status(Response.Status.NOT_FOUND).entity("File không tồn tại hoặc bạn không có quyền xem").build();
    }

    @GET
    @Path("/{id}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadFile(@PathParam("id") Long id, @Context ContainerRequestContext requestContext) {
        Object userIdObj = requestContext.getProperty("userId");
        if (userIdObj == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        
        Long userId = ((Number) userIdObj).longValue();
        Optional<FileMetadata> fileOpt = fileRepository.findById(id);
        
        // Kiểm tra quyền sở hữu
        if (fileOpt.isEmpty() || !fileOpt.get().getOwnerId().equals(userId)) {
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

   @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteFile(@PathParam("id") Long id) {
        try {
            FileMetadata target = fileRepository.findById(id).orElse(null);
            if (target == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            // Gọi hàm đệ quy để xóa tận gốc
            deleteFileAndChildrenRecursively(target);

            return Response.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    // Thêm hàm đệ quy này vào ngay bên dưới trong cùng class
    private void deleteFileAndChildrenRecursively(FileMetadata item) {
        if (Boolean.TRUE.equals(item.getIsFolder())) {
            List<FileMetadata> children = fileRepository.findByParentId(item.getId());
            for (FileMetadata child : children) {
                deleteFileAndChildrenRecursively(child);
            }
        } else {
            try {
                java.nio.file.Path physicalPath = java.nio.file.Paths.get(uploadDir, item.getFilePath());
                java.nio.file.Files.deleteIfExists(physicalPath);
                
                // THÊM ĐOẠN NÀY ĐỂ TRỪ DUNG LƯỢNG KHI XÓA FILE:
                User user = userRepository.findById(item.getOwnerId()).orElse(null);
                if (user != null && item.getFileSize() != null) {
                    long newQuota = Math.max(0, user.getUsedQuota() - item.getFileSize());
                    user.setUsedQuota(newQuota);
                    userRepository.save(user);
                }
                
            } catch (Exception e) {
                System.err.println("Không thể xóa file vật lý trên ổ cứng: " + item.getFilePath());
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

    // 2. API TẢI FILE CÔNG KHAI QUA TOKEN
    @GET
    @Path("/shared/{token}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM) // Đổi từ JSON sang OCTET_STREAM để tải file
    public Response downloadSharedFile(@PathParam("token") String token) {
        try {
            // Tìm file thông qua Token
            FileShare share = fileShareRepository.findByShareToken(token)
                    .orElseThrow(() -> new RuntimeException("Link chia sẻ không tồn tại hoặc đã hết hạn"));

            FileMetadata metadata = fileRepository.findById(share.getFileId())
                    .orElseThrow(() -> new RuntimeException("File gốc đã bị xóa"));

            // SỬA LỖI: Dùng metadata.getFilePath() thay vì getFileName()
            // và dùng Paths.get().resolve() để nối chuỗi đường dẫn chuẩn xác nhất
            java.nio.file.Path filePath = Paths.get(storageService.getUploadDir()).resolve(metadata.getFilePath());
            
            if (!Files.exists(filePath)) {
                return Response.status(Response.Status.NOT_FOUND).entity("File vật lý không tồn tại").build();
            }

            // Tạo luồng đọc file giống hệt hàm download thông thường
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

            // Trả về file cho trình duyệt tự động tải xuống
            return Response.ok(fileStream)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getFileName() + "\"")
                    .build();

        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
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
            
            List<FileMetadata> allFiles = fileRepository.findByOwnerId(userId);
            return Response.ok(allFiles).build();
            
        } catch (Exception e) {
            e.printStackTrace(); // In chi tiết lỗi ra màn hình Terminal của Backend
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/shared/by-me")
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

                fileRepository.findById(s.getFileId()).ifPresent(f -> dto.setFileName(f.getFileName()));

                if (s.getSharedWith() != null) {
                    userRepository.findById(s.getSharedWith()).ifPresent(u -> dto.setTargetEmail(u.getEmail()));
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
    @Path("/shared/with-me")
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

                fileRepository.findById(s.getFileId()).ifPresent(f -> dto.setFileName(f.getFileName()));
                
                userRepository.findById(s.getSharedBy()).ifPresent(u -> dto.setTargetEmail(u.getEmail()));
                result.add(dto);
            }
            return Response.ok(result).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("{\"error\": \"" + e.getMessage() + "\"}").build(); 
        }
    }

    // API THU HỒI QUYỀN CHIA SẺ
    @DELETE
    @Path("/shared/{shareId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response revokeShare(@PathParam("shareId") Long shareId, @Context ContainerRequestContext requestContext) {
        try {
            Long userId = ((Number) requestContext.getProperty("userId")).longValue();
            FileShare share = fileShareRepository.findById(shareId).orElse(null);
            
            // Chỉ chủ sở hữu (người tạo ra lượt share) mới được thu hồi
            if (share != null && share.getSharedBy().equals(userId)) {
                fileShareRepository.delete(share);
                return Response.ok("{\"message\": \"Đã thu hồi quyền truy cập\"}").build();
            }
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"Không có quyền thu hồi\"}").build();
        } catch (Exception e) { return Response.serverError().build(); }
    }
   
}