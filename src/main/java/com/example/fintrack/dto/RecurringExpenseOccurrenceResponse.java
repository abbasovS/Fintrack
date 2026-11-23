package com.example.fintrack.dto;

import com.example.fintrack.entity.RecurringExpenseOccurrenceStatus;

import java.time.LocalDate;

public record RecurringExpenseOccurrenceResponse(
        Long id,
        Long ruleId,
        LocalDate scheduledDate,
        RecurringExpenseOccurrenceStatus status,
        Long transactionId
) { }
