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
        if (userRepository.existsByUsername(user.getUsername())) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Username đã tồn tại").build();
        }
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
            String token = jwtUtil.generateToken(user.getUsername(), user.getId());
            return Response.ok(Map.of("token", token, "username", user.getUsername())).build();
        }
        
        return Response.status(Response.Status.UNAUTHORIZED).entity("Sai username hoặc password").build();
    }
}