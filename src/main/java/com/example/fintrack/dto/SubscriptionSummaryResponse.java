package com.example.fintrack.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionSummaryResponse(
        String currency,
        long activeCount,
        BigDecimal monthlyEquivalent,
        long upcomingCount,
        BigDecimal upcomingAmount,
        LocalDate monthStart,
        LocalDate monthEnd,
        BigDecimal actualIncome,
        BigDecimal actualExpense,
        BigDecimal plannedExpenses,
        BigDecimal availableAfterCommitments
) { }
