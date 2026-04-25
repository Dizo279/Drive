package com.filemanager.service;

import com.filemanager.entity.UpgradeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationService {

    private final List<SseEventSink> adminSinks = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper;
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
     * Gửi thông báo có yêu cầu nâng cấp mới đến tất cả Admin đang online
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
