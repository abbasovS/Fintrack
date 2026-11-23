package com.example.fintrack.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.Instant;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@Table(name = "users")
@Setter
@Getter
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 100)
    String name;
    @Column(nullable = false, length = 150, unique = true)
    String email;
    @Column(nullable = false, length = 255)
    String password;
    @Column(name = "email_verified", nullable = false)
    boolean emailVerified;
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;


    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
