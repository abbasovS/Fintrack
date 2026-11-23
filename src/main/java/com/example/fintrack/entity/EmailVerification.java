package com.example.fintrack.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Entity
@Table(name = "email_verifications")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailVerification {
    @Id
    @Column(length = 150)
    String email;
    @Column(nullable = false, length = 100)
    String name;
    @Column(name = "password_hash", nullable = false)
    String passwordHash;
    @Column(name = "code_hash", nullable = false, length = 100)
    String codeHash;
    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;
    @Column(name = "last_sent_at", nullable = false)
    Instant lastSentAt;
    @Column(nullable = false)
    int attempts;
}
