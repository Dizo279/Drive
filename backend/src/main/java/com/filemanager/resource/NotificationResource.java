package com.filemanager.resource;

import com.filemanager.entity.Notification;
import com.filemanager.entity.User;
import com.filemanager.repository.NotificationRepository;
import com.filemanager.repository.UserRepository;
import com.filemanager.service.NotificationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Path("/notifications")
public class NotificationResource {

    @Inject
    private NotificationRepository notificationRepository;

    @Inject
    private NotificationService notificationService;

    @Inject
    private UserRepository userRepository;

    // Helper lấy userId từ context
    private Long getCurrentUserId(ContainerRequestContext requestContext) {
        Object userIdObj = requestContext.getProperty("userId");
        if (userIdObj == null) return null;
        return ((Number) userIdObj).longValue();
    }

    // 1. Lấy danh sách thông báo của user hiện tại
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMyNotifications(@Context ContainerRequestContext requestContext) {
        Long userId = getCurrentUserId(requestContext);
        if (userId == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return Response.ok(notifications).build();
    }

    // 2. Đánh dấu thông báo đã đọc
    @PUT
    @Path("/{id}/read")
    @Produces(MediaType.APPLICATION_JSON)
    public Response markAsRead(@PathParam("id") Long id, @Context ContainerRequestContext requestContext) {
        Long userId = getCurrentUserId(requestContext);
        if (userId == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        Notification notification = notificationRepository.findByIdAndUserId(id, userId).orElse(null);
        if (notification == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        notification.setRead(true);
        notificationRepository.save(notification);
        return Response.ok("{\"message\": \"Đã đánh dấu đã đọc\"}").build();
    }

    // 3. Xóa thông báo
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteNotification(@PathParam("id") Long id, @Context ContainerRequestContext requestContext) {
        Long userId = getCurrentUserId(requestContext);
        if (userId == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        Notification notification = notificationRepository.findByIdAndUserId(id, userId).orElse(null);
        if (notification == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        notificationRepository.delete(notification);
        return Response.ok("{\"message\": \"Đã xóa thông báo\"}").build();
    }

    // 4. SSE - Real-time notifications cho user hiện tại
    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void subscribeToNotifications(@Context SseEventSink eventSink, @Context Sse sse, @Context ContainerRequestContext requestContext) {
        Long userId = getCurrentUserId(requestContext);
        if (userId == null) {
            eventSink.close();
            return;
        }
        notificationService.setSse(sse);
        notificationService.registerUser(userId, eventSink);
    }
}
