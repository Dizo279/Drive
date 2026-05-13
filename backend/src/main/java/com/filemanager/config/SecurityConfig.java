package com.filemanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
// Cấu hình bảo mật Spring Security, chủ yếu để kích hoạt CORS và cấu hình mã hóa mật khẩu
// Các API cụ thể sẽ được bảo vệ bởi JwtAuthFilter của JAX-RS, nên ở đây ta chỉ cần cấu hình cơ bản để cho phép CORS hoạt động tốt với Angular.
// cors là gì? CORS (Cross-Origin Resource Sharing) là một cơ chế bảo mật của trình duyệt cho phép hoặc từ chối các yêu cầu từ một nguồn khác (domain, protocol, hoặc port) so với nguồn của tài nguyên được yêu cầu. Trong trường hợp này, Angular chạy trên localhost:4200 sẽ gửi yêu cầu đến backend trên localhost:8080, nên cần cấu hình CORS để cho phép điều này.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Kích hoạt tính năng CORS bằng cấu hình bên dưới
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll() // Quyền truy cập API do JwtAuthFilter
            );
        return http.build();
    }

    // Bean định nghĩa luật CORS
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Cho phép frontend Angular (cổng 4200) gọi API
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
        
        // Cho phép các phương thức HTTP cơ bản, đặc biệt là OPTIONS cho pre-flight request
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Cho phép gửi kèm các header chứa Token
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Áp dụng luật này cho toàn bộ đường dẫn API
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}