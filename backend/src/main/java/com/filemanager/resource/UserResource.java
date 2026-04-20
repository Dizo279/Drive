package com.filemanager.resource;

import com.filemanager.entity.User;
import com.filemanager.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Component
@Path("/users")
public class UserResource {

    @Inject
    private UserRepository userRepository;

    @Inject
    private PasswordEncoder passwordEncoder;

    public static class ProfileUpdateRequest {
        public String fullName;
        public String avatarUrl;
        public String username;
        public String email;
        public String currentPassword;
        public String newPassword;
    }

    // 1. API Lấy thông tin cá nhân hiện tại
    @GET
    @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMyProfile(@Context ContainerRequestContext requestContext) {
        try {
            Object userIdObj = requestContext.getProperty("userId");
            if (userIdObj == null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\": \"Cần đăng nhập\"}").build();
            }
            Long userId = ((Number) userIdObj).longValue();
            
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"Không tìm thấy user\"}").build();
            }
            
            // Xóa password khỏi object trước khi gửi về Frontend để bảo mật
            user.setPassword(null);
            return Response.ok(user).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    @PUT
    @Path("/profile")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProfile(ProfileUpdateRequest updateData, @Context ContainerRequestContext requestContext) {
        try {
            Object userIdObj = requestContext.getProperty("userId");
            if (userIdObj == null) return Response.status(Response.Status.UNAUTHORIZED).build();
            Long userId = ((Number) userIdObj).longValue();
            
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return Response.status(Response.Status.NOT_FOUND).build();

            // 1. KIỂM TRA LOGIC THÔNG MINH HƠN:
            // Chỉ bắt isChanging... = true NẾU dữ liệu gửi lên THỰC SỰ KHÁC trong Database
            boolean isChangingUsername = updateData.username != null 
                    && !updateData.username.trim().isEmpty() 
                    && !updateData.username.equals(user.getUsername());
            
            // Xử lý cẩn thận trường hợp Email trong DB lúc đầu có thể bị null
            boolean currentEmailIsNull = user.getEmail() == null;
            boolean isChangingEmail = updateData.email != null 
                    && !updateData.email.trim().isEmpty() 
                    && (currentEmailIsNull || !updateData.email.equals(user.getEmail()));
            
            boolean isChangingPassword = updateData.newPassword != null 
                    && !updateData.newPassword.trim().isEmpty();

            // Đánh dấu xem có đang update thông tin nhạy cảm hay không
            boolean isUpdatingSensitiveInfo = isChangingUsername || isChangingEmail || isChangingPassword;

            if (isUpdatingSensitiveInfo) {
                // 1. KIỂM TRA MẬT KHẨU BẰNG HÀM MATCHES (Giống hệt lúc Login)
                if (updateData.currentPassword == null || !passwordEncoder.matches(updateData.currentPassword, user.getPassword())) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .type(MediaType.APPLICATION_JSON)
                            .entity("{\"error\": \"Mật khẩu hiện tại không đúng hoặc bị bỏ trống!\"}")
                            .build();
                }
                
                // Vượt qua bảo mật -> Lưu các thông tin nhạy cảm mới
                if (isChangingUsername) user.setUsername(updateData.username);
                if (isChangingEmail) user.setEmail(updateData.email);
                
                if (isChangingPassword) {
                    // 2. MÃ HÓA MẬT KHẨU MỚI BẰNG HÀM ENCODE TRƯỚC KHI LƯU (Giống hệt lúc Register)
                    user.setPassword(passwordEncoder.encode(updateData.newPassword));
                }
            }

            // 2. Cập nhật thông tin công khai (Tên, Ảnh) -> Được phép lưu thoải mái không cần mật khẩu
            if (updateData.fullName != null) user.setFullName(updateData.fullName);
            if (updateData.avatarUrl != null) user.setAvatarUrl(updateData.avatarUrl);

            userRepository.save(user);
            user.setPassword(null); // Giấu pass trước khi gửi về Frontend
            
            return Response.ok(user).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    // 3. API Nâng cấp gói cước lên PREMIUM (100GB)
    @POST
    @Path("/upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    public Response upgradeToPremium(@Context ContainerRequestContext requestContext) {
        try {
            Object userIdObj = requestContext.getProperty("userId");
            if (userIdObj == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
            Long userId = ((Number) userIdObj).longValue();
            
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return Response.status(Response.Status.NOT_FOUND).build();

            user.setTier("PREMIUM");
            // Set maxQuota lên 100GB (100 * 1024 * 1024 * 1024)
            user.setMaxQuota(107374182400L);
            
            userRepository.save(user);

            return Response.ok("{\"message\": \"Tài khoản đã được nâng cấp lên PREMIUM với 100GB dung lượng!\"}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/quota")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getQuota(@Context ContainerRequestContext requestContext) {
        try {
            Object userIdObj = requestContext.getProperty("userId");
            if (userIdObj == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
            Long userId = ((Number) userIdObj).longValue();
            
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return Response.status(Response.Status.NOT_FOUND).build();

            // Tính toán dung lượng
            long used = user.getUsedQuota() != null ? user.getUsedQuota() : 0L;
            long max = user.getMaxQuota() != null ? user.getMaxQuota() : 1073741824L; // 1GB mặc định
            long percentage = max > 0 ? (used * 100) / max : 0;

            // Tạo chuỗi JSON trả về cho Frontend
            String jsonResponse = String.format(
                "{\"usedQuota\": %d, \"maxQuota\": %d, \"percentage\": %d}", 
                used, max, percentage
            );
            
            return Response.ok(jsonResponse).build();
            
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }
}

