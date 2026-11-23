package com.example.fintrack.dto;

import com.example.fintrack.entity.CategoryType;

import java.math.BigDecimal;

public record CategoryReportResponse(
        Long categoryId,
        String categoryName,
        CategoryType type,
        BigDecimal totalAmount,
        long transactionCount
) { }
