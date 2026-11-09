package com.lgcns.jeonse.domain.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class Member {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private String email;
    @Column(name = "password_hash", nullable = false) private String passwordHash;
    @Column(nullable = false, length = 50) private String name;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    public Member(String email, String passwordHash, String name) {
        this.email = email; this.passwordHash = passwordHash; this.name = name;
        this.createdAt = LocalDateTime.now();
    }
}
