package com.example.fintrack.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ReportSummaryResponse(
        Instant from,
        Instant to,
        String currency,
        BigDecimal income,
        BigDecimal expense,
        BigDecimal balance,
        long transactionCount
) { }
