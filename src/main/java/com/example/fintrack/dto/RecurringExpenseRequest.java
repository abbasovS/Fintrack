package com.example.fintrack.dto;

import com.example.fintrack.entity.RecurringExpenseFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

public record RecurringExpenseRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull Long categoryId,
        @NotNull RecurringExpenseFrequency frequency,
        DayOfWeek dayOfWeek,
        @Min(1) @Max(31) Integer dayOfMonth,
        boolean skipWeekends,
        @NotNull @FutureOrPresent LocalDate startDate,
        @FutureOrPresent LocalDate endDate
) { }
