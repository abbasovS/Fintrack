package com.example.fintrack.dto;

import com.example.fintrack.entity.SubscriptionOccurrenceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionOccurrenceResponse(
        Long id,
        Long subscriptionId,
        String subscriptionName,
        LocalDate dueDate,
        BigDecimal expectedAmount,
        BigDecimal actualAmount,
        SubscriptionOccurrenceStatus status,
        LocalDate paidOn,
        Long transactionId,
        String currency
) { }
