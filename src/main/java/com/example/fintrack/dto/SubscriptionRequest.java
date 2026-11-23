package com.example.fintrack.dto;

import com.example.fintrack.entity.SubscriptionBillingInterval;
import com.example.fintrack.entity.SubscriptionStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull Long categoryId,
        @NotNull SubscriptionBillingInterval billingInterval,
        @NotNull LocalDate nextBillingDate,
        @NotNull SubscriptionStatus status,
        @Size(max = 500) String note
) { }
