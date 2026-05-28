package com.filemanager.resource;

import com.filemanager.entity.User;
import com.filemanager.repository.UserRepository;
import com.filemanager.security.JwtUtil;
import jakarta.inject.Inject;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    private UserRepository userRepository;

    @Inject
    private PasswordEncoder passwordEncoder;

    @Inject
    private JwtUtil jwtUtil;

    @POST
    @Path("/register")
    public Response register(User user) {
        // Validate required fields
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Họ tên không được để trống").build();
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Email không được để trống").build();
        }
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Tên đăng nhập không được để trống").build();
        }
        if (user.getPassword() == null || user.getPassword().trim().length() < 8) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Mật khẩu phải ít nhất 8 ký tự").build();
        }

        // Check duplicates
        if (userRepository.existsByUsername(user.getUsername())) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Tên đăng nhập đã tồn tại").build();
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Email đã được sử dụng").build();
        }

        // Trim inputs (NO normalize case to keep username case-sensitive)
        user.setFullName(user.getFullName().trim());
        user.setEmail(user.getEmail().trim());
        user.setUsername(user.getUsername().trim());


        // Encode password and save
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        
        return Response.status(Response.Status.CREATED).entity("Đăng ký thành công").build();
    }

    @POST
    @Path("/login")
    public Response login(User loginRequest) {
        Optional<User> userOpt = userRepository.findByUsername(loginRequest.getUsername());
        
        if (userOpt.isPresent() && passwordEncoder.matches(loginRequest.getPassword(), userOpt.get().getPassword())) {
            User user = userOpt.get();
            String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getId(), user.getRole());
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), user.getId(), user.getRole());

            NewCookie refreshCookie = new NewCookie.Builder("refresh_token")
                    .value(refreshToken)
                    .path("/api/auth")
                    .httpOnly(true)
                    .secure(true)
                    .maxAge(7 * 24 * 60 * 60)
                    .build();

            return Response.ok(Map.of("accessToken", accessToken, "username", user.getUsername(), "role", user.getRole()))
                    .cookie(refreshCookie)
                    .build();
        }

        return Response.status(Response.Status.UNAUTHORIZED).entity("Sai username hoặc password").build();
    }

    @POST
    @Path("/refresh")
    public Response refresh(@CookieParam("refresh_token") Cookie refreshCookie) {
        if (refreshCookie == null || refreshCookie.getValue() == null || refreshCookie.getValue().isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Thiếu refresh token").build();
        }

        try {
            Claims claims = jwtUtil.validateRefreshTokenAndGetClaims(refreshCookie.getValue());
            String username = claims.getSubject();
            Long userId = claims.get("userId", Number.class).longValue();
            User user = userRepository.findById(userId).orElseThrow(() -> new JwtException("User not found"));
            String accessToken = jwtUtil.generateAccessToken(username, userId, user.getRole());
            return Response.ok(Map.of("accessToken", accessToken)).build();
        } catch (JwtException ex) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Refresh token không hợp lệ").build();
        }
    }

    @POST
    @Path("/logout")
    public Response logout() {
        NewCookie clearCookie = new NewCookie.Builder("refresh_token")
                .value("")
                .path("/api/auth")
                .httpOnly(true)
                .secure(true)
                .maxAge(0)
                .build();

        return Response.ok("Đăng xuất thành công").cookie(clearCookie).build();
    }
}