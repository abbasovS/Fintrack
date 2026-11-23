package com.example.fintrack.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "subscription_occurrences")
public class SubscriptionOccurrence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    Subscription subscription;

    @Column(name = "due_date", nullable = false)
    LocalDate dueDate;

    @Column(name = "expected_amount", nullable = false, precision = 19, scale = 2)
    BigDecimal expectedAmount;

    @Column(name = "actual_amount", precision = 19, scale = 2)
    BigDecimal actualAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    SubscriptionOccurrenceStatus status;

    @Column(name = "paid_on")
    LocalDate paidOn;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", unique = true)
    Transaction transaction;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
