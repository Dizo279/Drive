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

import jakarta.ws.rs.core.Response;
import com.filemanager.entity.FileMetadata;
import com.filemanager.entity.User;

@Component
@Path("/files")
public class FileResource {

    @Inject
    private StorageService storageService;

    @Inject
    private FileRepository fileRepository;

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
            
            fileRepository.save(metadata);
            
            return Response.ok(metadata).build();
        } catch (WebApplicationException we) {
            throw we; // Ném tiếp lỗi Quota ra ngoài
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("Lỗi upload: " + e.getMessage()).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMyFiles(@Context ContainerRequestContext requestContext) {
        Object userIdObj = requestContext.getProperty("userId");
        if (userIdObj == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Cần đăng nhập").build();
        }
        
        Long userId = ((Number) userIdObj).longValue();
        List<FileMetadata> files = fileRepository.findByOwnerId(userId);
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
    public Response deleteFile(@PathParam("id") Long id) {
        try {
            // 1. Dùng đúng tên Entity là FileMetadata
            FileMetadata file = fileRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy file"));

            // 2. Hoàn lại dung lượng (Quota) cho User
            Long ownerId = file.getOwnerId(); // Sử dụng hàm getOwnerId() từ FileMetadata
            
            if (ownerId != null) {
                // Lấy đối tượng User từ Database thông qua ID
                User user = userRepository.findById(ownerId).orElse(null);
                
                if (user != null && user.getUsedQuota() != null) {
                    long newQuota = user.getUsedQuota() - file.getFileSize();
                    user.setUsedQuota(Math.max(0L, newQuota)); // Đảm bảo không bị âm
                    userRepository.save(user);
                }
            }

            // 3. Xóa file vật lý trên ổ cứng qua StorageService
            storageService.deletePhysicalFile(file.getFileName());

            // 4. Xóa bản ghi trong Database
            fileRepository.delete(file);

            // Trả về JAX-RS Response thay vì ResponseEntity
            return Response.ok("{\"message\": \"Xóa file thành công\"}").build();
            
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

   // 1. API TẠO LINK CHIA SẺ
    @POST
    @Path("/{id}/share")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createShareLink(@PathParam("id") Long fileId, @Context ContainerRequestContext requestContext) {
        try {
            Object userIdObj = requestContext.getProperty("userId");
            if (userIdObj == null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\": \"Cần đăng nhập\"}").build();
            }
            Long userId = ((Number) userIdObj).longValue();

            FileMetadata file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy file"));

            // BẢO MẬT & CHỐNG LỖI: Kiểm tra ownerId có bị null không (với các file cũ)
            if (file.getOwnerId() == null || !file.getOwnerId().equals(userId)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("{\"error\": \"Bạn không có quyền chia sẻ file này (hoặc file không có chủ sở hữu)\"}").build();
            }

            FileShare existingShare = fileShareRepository.findByFileId(fileId).orElse(null);
            if (existingShare != null) {
                return Response.ok("{\"shareToken\": \"" + existingShare.getShareToken() + "\"}").build();
            }

            FileShare newShare = new FileShare();
            newShare.setFileId(fileId);
            newShare.setShareToken(UUID.randomUUID().toString());
            newShare.setCreatedAt(LocalDateTime.now());
            newShare.setSharedBy(userId);
            newShare.setExpiresAt(LocalDateTime.now().plusDays(7));
            
            fileShareRepository.save(newShare);

            return Response.ok("{\"shareToken\": \"" + newShare.getShareToken() + "\"}").build();
            
        } catch (Exception e) {
            // IN LỖI RA TERMINAL ĐỂ DỄ DÀNG TÌM DIỆT
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
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
}