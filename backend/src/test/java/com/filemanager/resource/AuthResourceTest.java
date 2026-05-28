package com.filemanager.resource;

import com.filemanager.entity.User;
import com.filemanager.repository.UserRepository;
import com.filemanager.security.JwtUtil;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthResourceTest {

    private AuthResource authResource;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        authResource = new AuthResource();
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtUtil = new JwtUtil();

        ReflectionTestUtils.setField(jwtUtil, "secret", "9a4f2c8d3b7a1e6f45c8a0b3f267d8b1d4e6f3c8a9d2b5f8e3a9c8b5f6v8a3d9");
        ReflectionTestUtils.setField(jwtUtil, "accessExpirationMs", 1800000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpirationMs", 604800000L);
        jwtUtil.init();

        ReflectionTestUtils.setField(authResource, "userRepository", userRepository);
        ReflectionTestUtils.setField(authResource, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(authResource, "jwtUtil", jwtUtil);
    }

    @Test
    void loginShouldReturnAccessTokenAndSetRefreshCookie() {
        User existing = new User();
        existing.setUsername("alice");
        existing.setPassword("encoded");
        ReflectionTestUtils.setField(existing, "id", 1L);

        User request = new User();
        request.setUsername("alice");
        request.setPassword("Password123");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("Password123", "encoded")).thenReturn(true);

        Response response = authResource.login(request);

        assertEquals(200, response.getStatus());
        Map<?, ?> body = (Map<?, ?>) response.getEntity();
        assertNotNull(body.get("accessToken"));
        assertEquals("alice", body.get("username"));

        NewCookie refreshCookie = response.getCookies().get("refresh_token");
        assertNotNull(refreshCookie);
        assertEquals(604800, refreshCookie.getMaxAge());
    }

    @Test
    void refreshShouldReturnNewAccessTokenWhenCookieValid() {
        User existing = new User();
        existing.setUsername("alice");
        existing.setRole("USER");
        ReflectionTestUtils.setField(existing, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        String refreshToken = jwtUtil.generateRefreshToken("alice", 1L);

        Response response = authResource.refresh(new Cookie("refresh_token", refreshToken));

        assertEquals(200, response.getStatus());
        Map<?, ?> body = (Map<?, ?>) response.getEntity();
        assertNotNull(body.get("accessToken"));
    }

    @Test
    void refreshShouldReturn401WhenCookieMissing() {
        Response response = authResource.refresh(null);

        assertEquals(401, response.getStatus());
    }

    @Test
    void logoutShouldClearRefreshCookie() {
        Response response = authResource.logout();

        assertEquals(200, response.getStatus());
        NewCookie refreshCookie = response.getCookies().get("refresh_token");
        assertNotNull(refreshCookie);
        assertTrue(refreshCookie.getMaxAge() == 0);
    }
}
