package com.filemanager.filter;

import com.filemanager.security.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthFilter implements ContainerRequestFilter {

    @Inject
    private JwtUtil jwtUtil;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // 1. Cho phép mọi request OPTIONS (Pre-flight CORS) đi qua mà không cần check token
        if (requestContext.getMethod().equalsIgnoreCase("OPTIONS")) {
            return;
        }

        // Lấy đường dẫn thực tế mà hệ thống nhận được
        String path = requestContext.getUriInfo().getPath();
        
        System.out.println("-----> [JwtFilter] Đang kiểm tra đường dẫn: " + path);

        //"shared/" (có dấu /) để phân biệt hoàn toàn với "shared-by-me"
        if (path.contains("auth") || path.contains("shared/")) {
            return; // Mở cổng cho phép tải file mà không cần Token
        }

        // 3. Kiểm tra Token cho các request còn lại
        String token = null;
        String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);// Lấy token từ header Authorization

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else {
            // Hỗ trợ SSE: Token có thể được truyền qua query parameter
            token = requestContext.getUriInfo().getQueryParameters().getFirst("token");
        }

        if (token == null) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Thiếu hoặc sai định dạng Authorization header").build());
            return;
        }
        // 4. Validate token và lấy thông tin userId, username từ token
        // lấy thông tin userId và username từ token để lưu vào context, giúp các endpoint khác có thể sử dụng mà không cần phải decode lại token nhiều lần
        try {
            Claims claims = jwtUtil.validateAccessTokenAndGetClaims(token);
            // Lưu userId vào context để các endpoint khác sử dụng
            requestContext.setProperty("userId", claims.get("userId"));
            requestContext.setProperty("username", claims.getSubject());
        } catch (Exception e) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Token không hợp lệ hoặc đã hết hạn").build());
        }
    }
}
