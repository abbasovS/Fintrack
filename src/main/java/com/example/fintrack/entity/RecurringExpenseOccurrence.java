package com.example.fintrack.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "recurring_expense_occurrences", uniqueConstraints =
        @UniqueConstraint(name = "uk_recurring_expense_occurrence_rule_date", columnNames = {"rule_id", "scheduled_date"}))
public class RecurringExpenseOccurrence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    RecurringExpenseRule rule;

    @Column(name = "scheduled_date", nullable = false)
    LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    RecurringExpenseOccurrenceStatus status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", unique = true)
    Transaction transaction;
}
