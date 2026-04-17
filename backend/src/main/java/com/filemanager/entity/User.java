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

    @Column(name = "max_quota")
    private Long maxQuota = 1073741824L; // Mặc định 1GB

    @Column(name = "used_quota")
    private Long usedQuota = 0L;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getter và Setter (bạn tự generate bằng IDE hoặc dùng Lombok @Data nhé)
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Long getUsedQuota() {return usedQuota;}
    public void setUsedQuota(Long usedQuota) {this.usedQuota = usedQuota;}
    public Long getMaxQuota() {return maxQuota;}
    public void setMaxQuota(Long maxQuota) {this.maxQuota = maxQuota;}
}