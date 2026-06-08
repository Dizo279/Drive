package com.filemanager.config;

import com.filemanager.filter.JwtAuthFilter;
import com.filemanager.filter.CorsFilter;
import com.filemanager.resource.AuthResource;
import com.filemanager.resource.FileResource;
import com.filemanager.resource.UserResource;

import jakarta.ws.rs.Path;

import com.filemanager.resource.AdminResource;
import com.filemanager.resource.NotificationResource;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.media.sse.SseFeature;
import org.springframework.context.annotation.Configuration;

//JerseyConfig đóng vai trò là nơi khai báo các thành phần trực tiếp tham gia vào việc nhận và xử lý yêu cầu HTTP.
//Chỉ những lớp định nghĩa các đường dẫn API (@Path) hoặc can thiệp vào luồng request/response (Filter) mới cần đăng ký với Jersey


@Configuration
public class JerseyConfig extends ResourceConfig {
    public JerseyConfig() {
        // Đăng ký các Resource và Filter
        register(AuthResource.class);
        register(FileResource.class);
        register(JwtAuthFilter.class);
        register(CorsFilter.class);
        
        // Kích hoạt tính năng Upload File (Multipart)
        register(MultiPartFeature.class);
        // Kích hoạt SSE (Server-Sent Events) cho thông báo real-time
        register(SseFeature.class);
        register(UserResource.class);
        register(AdminResource.class);
        register(NotificationResource.class);
        
    }
}

