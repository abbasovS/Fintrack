package com.example.fintrack.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MarkSubscriptionPaidRequest(
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal actualAmount,
        @NotNull Long expenseCategoryId,
        @NotNull @PastOrPresent LocalDate paidOn,
        String note
) { }
