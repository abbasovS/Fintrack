package com.example.fintrack.dto;

import com.example.fintrack.entity.RecurringExpenseFrequency;
import com.example.fintrack.entity.RecurringExpenseStatus;
import com.example.fintrack.entity.CategoryType;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

public record RecurringExpenseResponse(
        Long id,
        String name,
        BigDecimal amount,
        String currency,
        Long categoryId,
        String categoryName,
        CategoryType categoryType,
        RecurringExpenseFrequency frequency,
        DayOfWeek dayOfWeek,
        Integer dayOfMonth,
        boolean skipWeekends,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate nextRunDate,
        RecurringExpenseStatus status
) { }
