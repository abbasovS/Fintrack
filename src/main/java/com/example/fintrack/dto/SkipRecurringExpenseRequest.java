package com.example.fintrack.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SkipRecurringExpenseRequest(@NotNull LocalDate date) { }
