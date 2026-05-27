package com.filemanager.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-expiration-ms}")
    private long accessExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private Key key;

    @PostConstruct
    public void init() {
        // Khởi tạo key từ chuỗi secret trong application.properties
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(String username, Long userId) {
        return buildToken(username, userId, "access", accessExpirationMs);
    }

    public String generateRefreshToken(String username, Long userId) {
        return buildToken(username, userId, "refresh", refreshExpirationMs);
    }

    public String generateToken(String username, Long userId) {
        return generateAccessToken(username, userId);
    }

    public Claims validateTokenAndGetClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Claims validateAccessTokenAndGetClaims(String token) {
        Claims claims = validateTokenAndGetClaims(token);
        if (!"access".equals(claims.get("type", String.class))) {
            throw new io.jsonwebtoken.JwtException("Invalid token type");
        }
        return claims;
    }

    public Claims validateRefreshTokenAndGetClaims(String token) {
        Claims claims = validateTokenAndGetClaims(token);
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new io.jsonwebtoken.JwtException("Invalid token type");
        }
        return claims;
    }

    private String buildToken(String username, Long userId, String type, long expirationMs) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("type", type)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}