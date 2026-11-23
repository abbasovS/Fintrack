package com.example.fintrack.dto;

import com.example.fintrack.entity.CategoryType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record TransactionResponse(
        Long id,
        Long userId,
        Long categoryId,
        String categoryName,
        BigDecimal amount,
        CategoryType type,
        String description,
        Instant createdAt
) {
}
