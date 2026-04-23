package com.filemanager.resource;

import com.filemanager.entity.User;
import com.filemanager.repository.UserRepository;

import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

@Component
@Path("/admin") // Khớp chính xác với đường dẫn Angular đang gọi
public class AdminResource {

    @Inject
    private UserRepository userRepository;

    // Hàm dùng chung để kiểm tra quyền Admin cực kỳ bảo mật
    private boolean isAdmin(ContainerRequestContext requestContext) {
        Object userIdObj = requestContext.getProperty("userId");
        if (userIdObj == null) return false;
        
        Long userId = ((Number) userIdObj).longValue();
        User user = userRepository.findById(userId).orElse(null);
        
        // Trả về true nếu user tồn tại và có role là ADMIN
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    // 1. API Lấy danh sách toàn bộ người dùng
    @GET
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllUsers(@Context ContainerRequestContext requestContext) {
        if (!isAdmin(requestContext)) {
            return Response.status(Response.Status.FORBIDDEN)
                           .entity("{\"error\": \"Bạn không có quyền quản trị viên\"}").build();
        }
        
        List<User> users = userRepository.findAll();
        return Response.ok(users).build();
    }

    // 2. API Đổi Role (Quyền)
    @PUT
    @Path("/users/{userId}/role")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateRole(@PathParam("userId") Long targetUserId, Map<String, String> body, @Context ContainerRequestContext requestContext) {
        if (!isAdmin(requestContext)) return Response.status(Response.Status.FORBIDDEN).build();
        
        User targetUser = userRepository.findById(targetUserId).orElse(null);
        if (targetUser == null) return Response.status(Response.Status.NOT_FOUND).build();
        
        targetUser.setRole(body.get("role"));
        userRepository.save(targetUser);
        
        return Response.ok("{\"message\": \"Cập nhật quyền thành công\"}").build();
    }

    // 3. API Đổi Tier (Gói dung lượng)
    @PUT
    @Path("/users/{userId}/tier")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateTier(@PathParam("userId") Long targetUserId, Map<String, String> body, @Context ContainerRequestContext requestContext) {
        if (!isAdmin(requestContext)) return Response.status(Response.Status.FORBIDDEN).build();
        
        User targetUser = userRepository.findById(targetUserId).orElse(null);
        if (targetUser == null) return Response.status(Response.Status.NOT_FOUND).build();
        
        targetUser.setTier(body.get("tier"));
        userRepository.save(targetUser);
        
        return Response.ok("{\"message\": \"Cập nhật gói thành công\"}").build();
    }

    // 4. API Xóa người dùng
    @DELETE
    @Path("/users/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUser(@PathParam("userId") Long targetUserId, @Context ContainerRequestContext requestContext) {
        if (!isAdmin(requestContext)) return Response.status(Response.Status.FORBIDDEN).build();
        
        // Lưu ý: Tùy thuộc vào logic database của bạn, nếu User có chứa File, 
        // bạn có thể cần xóa File trước khi xóa User để tránh lỗi Foreign Key.
        userRepository.deleteById(targetUserId);
        
        return Response.ok("{\"message\": \"Xóa người dùng thành công\"}").build();
    }
}