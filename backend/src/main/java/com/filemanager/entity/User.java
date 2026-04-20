package com.filemanager.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    // --- CÁC CỘT MỚI BỔ SUNG CHO PROFILE ---
    @Column(unique = true, length = 100)
    private String email;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Lob
    @Column(name = "avatar_url", columnDefinition = "LONGTEXT")
    private String avatarUrl;

    @Column(length = 20)
    private String role = "USER"; // "USER" hoặc "ADMIN"

    @Column(length = 20)
    private String tier = "FREE"; // "FREE" hoặc "PREMIUM"

    @Column(name = "max_quota")
    private Long maxQuota = 1073741824L; // Mặc định 1GB

    @Column(name = "used_quota")
    private Long usedQuota = 0L;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // --- GETTER VÀ SETTER ---
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Long getUsedQuota() { return usedQuota; }
    public void setUsedQuota(Long usedQuota) { this.usedQuota = usedQuota; }
    public Long getMaxQuota() { return maxQuota; }
    public void setMaxQuota(Long maxQuota) { this.maxQuota = maxQuota; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }
}