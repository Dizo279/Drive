package com.filemanager.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "9a4f2c8d3b7a1e6f45c8a0b3f267d8b1d4e6f3c8a9d2b5f8e3a9c8b5f6v8a3d9");
        ReflectionTestUtils.setField(jwtUtil, "accessExpirationMs", 1800000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpirationMs", 604800000L);
        jwtUtil.init();
    }

    @Test
    void shouldGenerateAccessTokenWithAccessTypeClaim() {
        String token = jwtUtil.generateAccessToken("alice", 1L);
        Claims claims = jwtUtil.validateAccessTokenAndGetClaims(token);

        assertEquals("alice", claims.getSubject());
        assertEquals("access", claims.get("type", String.class));
    }

    @Test
    void shouldGenerateRefreshTokenWithRefreshTypeClaim() {
        String token = jwtUtil.generateRefreshToken("alice", 1L);
        Claims claims = jwtUtil.validateRefreshTokenAndGetClaims(token);

        assertEquals("alice", claims.getSubject());
        assertEquals("refresh", claims.get("type", String.class));
    }

    @Test
    void shouldRejectRefreshValidationForAccessToken() {
        String accessToken = jwtUtil.generateAccessToken("alice", 1L);

        assertThrows(JwtException.class, () -> jwtUtil.validateRefreshTokenAndGetClaims(accessToken));
    }

    @Test
    void shouldGenerateAccessTokenWithRoleClaim() {
        String token = jwtUtil.generateAccessToken("alice", 1L, "ADMIN");
        Claims claims = jwtUtil.validateAccessTokenAndGetClaims(token);

        assertEquals("alice", claims.getSubject());
        assertEquals("access", claims.get("type", String.class));
        assertEquals("ADMIN", claims.get("role", String.class));
    }
}
