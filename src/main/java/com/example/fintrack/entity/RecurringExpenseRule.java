package com.example.fintrack.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "recurring_expense_rules")
public class RecurringExpenseRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    Category category;

    @Column(nullable = false, length = 120)
    String name;

    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    RecurringExpenseFrequency frequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 10)
    DayOfWeek dayOfWeek;

    @Column(name = "day_of_month")
    Integer dayOfMonth;

    @Column(name = "skip_weekends", nullable = false)
    boolean skipWeekends;

    @Column(name = "start_date", nullable = false)
    LocalDate startDate;

    @Column(name = "end_date")
    LocalDate endDate;

    @Column(name = "next_run_date", nullable = false)
    LocalDate nextRunDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    RecurringExpenseStatus status;
}
