package com.example.fintrack.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MonthlyBudgetResponse(
        Long id,
        Long categoryId,
        String categoryName,
        LocalDate monthStart,
        BigDecimal amount,
        BigDecimal actualAmount,
        BigDecimal remainingAmount,
        int usagePercent,
        String currency
) { }
