package com.filemanager.config;

import com.filemanager.filter.JwtAuthFilter;
import com.filemanager.filter.CorsFilter;
import com.filemanager.resource.AuthResource;
import com.filemanager.resource.FileResource;
import com.filemanager.resource.UserResource;
import com.filemanager.resource.AdminResource;
import com.filemanager.resource.NotificationResource;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.media.sse.SseFeature;
import org.springframework.context.annotation.Configuration;

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
        // K�ch ho?t SSE (Server-Sent Events) cho th�ng b�o real-time
        register(SseFeature.class);
        register(UserResource.class);
        register(AdminResource.class);
        register(NotificationResource.class);
        
    }
}

