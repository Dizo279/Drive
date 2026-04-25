package com.filemanager.resource;

import com.filemanager.entity.User;
import com.filemanager.entity.UpgradeRequest;
import com.filemanager.repository.UserRepository;
import com.filemanager.repository.UpgradeRequestRepository;

import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import com.filemanager.service.NotificationService;
import java.util.List;
import java.util.Map;

@Component
@Path("/admin") // Khớp chính xác với đường dẫn Angular đang gọi
public class AdminResource {

    @Inject
    private UserRepository userRepository;

    @Inject
    private UpgradeRequestRepository upgradeRequestRepository;

    @Inject
    private NotificationService notificationService;

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
        
        String newTier = body.get("tier");
        targetUser.setTier(newTier);
        if ("PREMIUM".equalsIgnoreCase(newTier)) {
            targetUser.setMaxQuota(107374182400L); // 100GB
        } else {
            targetUser.setMaxQuota(1073741824L); // 1GB
        }
        userRepository.save(targetUser);
        
        return Response.ok("{\"message\": \"Cập nhật gói thành công\"}").build();
    }

    // 4. API Xóa người dùng
    // 5. API Lấy danh sách yêu cầu nâng cấp đang chờ
    @GET
    @Path("/upgrade-requests")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUpgradeRequests(@Context ContainerRequestContext requestContext) {
        if (!isAdmin(requestContext)) return Response.status(Response.Status.FORBIDDEN).build();
        return Response.ok(upgradeRequestRepository.findByStatus("PENDING")).build();
    }

    // 6. API Phê duyệt/Từ chối yêu cầu nâng cấp
    @POST
    @Path("/upgrade-requests/{id}/process")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response processUpgradeRequest(@PathParam("id") Long requestId, Map<String, String> body, @Context ContainerRequestContext requestContext) {
        if (!isAdmin(requestContext)) return Response.status(Response.Status.FORBIDDEN).build();
        
        UpgradeRequest request = upgradeRequestRepository.findById(requestId).orElse(null);
        if (request == null) return Response.status(Response.Status.NOT_FOUND).build();
        
        String action = body.get("action"); // "APPROVE" hoặc "REJECT"
        if ("APPROVE".equalsIgnoreCase(action)) {
            User user = userRepository.findById(request.getUserId()).orElse(null);
            if (user != null) {
                user.setTier("PREMIUM");
                user.setMaxQuota(107374182400L); // 100GB
                userRepository.save(user);
            }
            request.setStatus("APPROVED");
        } else {
            request.setStatus("REJECTED");
        }
        
        upgradeRequestRepository.save(request);
        return Response.ok("{\"message\": \"Đã xử lý yêu cầu nâng cấp\"}").build();
    }

    // 7. API SSE - Real-time notifications cho Admin
    @GET
    @Path("/sse/notifications")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void subscribeToNotifications(@Context SseEventSink eventSink, @Context Sse sse, @Context ContainerRequestContext requestContext) {
        if (!isAdmin(requestContext)) {
            eventSink.close();
            return;
        }
        notificationService.setSse(sse);
        notificationService.registerAdmin(eventSink);
    }
}
