package com.filemanager.config;

import com.filemanager.filter.JwtAuthFilter;
import com.filemanager.resource.AuthResource;
import com.filemanager.resource.FileResource;
import com.filemanager.resource.UserResource;
import com.filemanager.resource.AdminResource;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JerseyConfig extends ResourceConfig {
    public JerseyConfig() {
        // Đăng ký các Resource và Filter
        register(AuthResource.class);
        register(FileResource.class);
        register(JwtAuthFilter.class);
        
        // Kích hoạt tính năng Upload File (Multipart)
        register(MultiPartFeature.class);
        register(UserResource.class); // Đăng ký UserResource để quản lý người dùng (nếu có)
        register(AdminResource.class); // Đăng ký AdminResource để quản lý admin (nếu có)

    }
}
