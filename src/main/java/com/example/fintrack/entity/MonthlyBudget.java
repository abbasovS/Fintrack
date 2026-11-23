package com.example.fintrack.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "monthly_budgets", uniqueConstraints = @UniqueConstraint(
        name = "uk_monthly_budget_user_category_month", columnNames = {"user_id", "category_id", "month_start"}))
public class MonthlyBudget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    Category category;

    @Column(name = "month_start", nullable = false)
    LocalDate monthStart;

    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal amount;
}
