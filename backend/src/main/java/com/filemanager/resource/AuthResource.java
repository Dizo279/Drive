package com.filemanager.resource;

import com.filemanager.entity.User;
import com.filemanager.repository.UserRepository;
import com.filemanager.security.JwtUtil;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
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

        // Trim and normalize inputs
        user.setFullName(user.getFullName().trim());
        user.setEmail(user.getEmail().trim().toLowerCase());
        user.setUsername(user.getUsername().trim().toLowerCase());

        // Encode password and save — luôn là tài khoản thường, không cho đăng ký Admin qua API
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER");
        user.setTier("FREE");
        userRepository.save(user);
        
        return Response.status(Response.Status.CREATED).entity("Đăng ký thành công").build();
    }

    @POST
    @Path("/login")
    public Response login(User loginRequest) {
        Optional<User> userOpt = userRepository.findByUsername(loginRequest.getUsername());
        
        if (userOpt.isPresent() && passwordEncoder.matches(loginRequest.getPassword(), userOpt.get().getPassword())) {
            User user = userOpt.get();
            String token = jwtUtil.generateToken(user.getUsername(), user.getId());
            return Response.ok(Map.of(
                    "token", token,
                    "username", user.getUsername(),
                    "role", user.getRole() != null ? user.getRole() : "USER"
            )).build();
        }
        
        return Response.status(Response.Status.UNAUTHORIZED).entity("Sai username hoặc password").build();
    }
}