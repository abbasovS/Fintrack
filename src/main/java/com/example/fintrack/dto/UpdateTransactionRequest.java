package com.example.fintrack.dto;

import com.example.fintrack.entity.CategoryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateTransactionRequest(

        @NotNull(message = "Category ID is required")
        Long categoryId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Transaction type is required")
        CategoryType type,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description
) {
}
