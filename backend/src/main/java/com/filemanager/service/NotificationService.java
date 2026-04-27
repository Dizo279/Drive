package com.filemanager.service;

import com.filemanager.entity.Notification;
import com.filemanager.entity.UpgradeRequest;
import com.filemanager.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationService {

    private final List<SseEventSink> adminSinks = new CopyOnWriteArrayList<>();
    private final Map<Long, List<SseEventSink>> userSinks = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    private Sse sse;

    public NotificationService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    /**
     * Được gọi từ AdminResource khi có Admin kết nối SSE
     */
    public synchronized void setSse(Sse sse) {
        if (this.sse == null) {
            this.sse = sse;
        }
    }

    /**
     * Đăng ký một Admin vào danh sách nhận thông báo
     */
    public void registerAdmin(SseEventSink sink) {
        adminSinks.add(sink);
    }

    /**
     * Đăng ký một user bất kỳ vào danh sách nhận thông báo real-time
     */
    public void registerUser(Long userId, SseEventSink sink) {
        userSinks.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(sink);
    }

    /**
     * Lưu notification và gửi real-time đến user cụ thể
     */
    public void sendNotification(Notification notification) {
        notificationRepository.save(notification);
        pushToUser(notification.getUserId(), notification);
    }

    /**
     * Lưu notification và gửi real-time đến tất cả admin
     */
    public void sendNotificationToAdmins(Notification notification) {
        notificationRepository.save(notification);
        pushToAdmins(notification);
    }

    /**
     * Gửi thông báo có yêu cầu nâng cấp mới đến tất cả Admin đang online
     * (giữ lại để tương thích ngược với admin dashboard hiện tại)
     */
    public void notifyNewUpgradeRequest(UpgradeRequest request) {
        if (sse == null || adminSinks.isEmpty()) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(request);
            OutboundSseEvent event = sse.newEventBuilder()
                    .name("upgrade-request")
                    .data(String.class, json)
                    .build();

            sendToAllAdmins(event);
        } catch (Exception e) {
            System.err.println("Lỗi khi gửi SSE notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gửi thông báo chung (có thể dùng cho các loại thông báo khác trong tương lai)
     */
    public void notifyAdmins(String eventName, String message) {
        if (sse == null || adminSinks.isEmpty()) {
            return;
        }

        try {
            OutboundSseEvent event = sse.newEventBuilder()
                    .name(eventName)
                    .data(String.class, message)
                    .build();

            sendToAllAdmins(event);
        } catch (Exception e) {
            System.err.println("Lỗi khi gửi SSE notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void pushToUser(Long userId, Notification notification) {
        List<SseEventSink> sinks = userSinks.get(userId);
        if (sinks == null || sinks.isEmpty()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(notification);
            OutboundSseEvent event = sse.newEventBuilder()
                    .name("notification")
                    .data(String.class, json)
                    .build();
            for (SseEventSink sink : sinks) {
                try {
                    if (!sink.isClosed()) {
                        sink.send(event);
                    } else {
                        sinks.remove(sink);
                    }
                } catch (Exception e) {
                    sinks.remove(sink);
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi gửi SSE notification đến user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void pushToAdmins(Notification notification) {
        if (sse == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(notification);
            OutboundSseEvent event = sse.newEventBuilder()
                    .name("notification")
                    .data(String.class, json)
                    .build();
            sendToAllAdmins(event);
            // Also push to admin user sinks if any
            for (Map.Entry<Long, List<SseEventSink>> entry : userSinks.entrySet()) {
                // We don't know which users are admin here, so push to all registered admin sinks
                // Admin sinks in userSinks will receive it too
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi gửi SSE notification đến admins: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendToAllAdmins(OutboundSseEvent event) {
        for (SseEventSink sink : adminSinks) {
            try {
                if (!sink.isClosed()) {
                    sink.send(event);
                } else {
                    adminSinks.remove(sink);
                }
            } catch (Exception e) {
                adminSinks.remove(sink);
            }
        }
    }
}
