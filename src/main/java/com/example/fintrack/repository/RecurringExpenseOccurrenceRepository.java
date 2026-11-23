package com.example.fintrack.repository;

import com.example.fintrack.entity.RecurringExpenseOccurrence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface RecurringExpenseOccurrenceRepository extends JpaRepository<RecurringExpenseOccurrence, Long> {
    Optional<RecurringExpenseOccurrence> findByRuleIdAndScheduledDate(Long ruleId, LocalDate scheduledDate);
}
