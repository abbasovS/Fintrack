package com.example.fintrack.dto;

import com.example.fintrack.entity.SubscriptionBillingInterval;
import com.example.fintrack.entity.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SubscriptionResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        BigDecimal amount,
        String currency,
        SubscriptionBillingInterval billingInterval,
        LocalDate nextBillingDate,
        SubscriptionStatus status,
        String note,
        BigDecimal monthlyEquivalent,
        Long nextOccurrenceId,
        Instant createdAt,
        Instant updatedAt
) { }
