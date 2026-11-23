package com.example.fintrack.repository;

import com.example.fintrack.entity.CategoryType;

import java.math.BigDecimal;

public interface CategoryReportProjection {
    Long getCategoryId();
    String getCategoryName();
    CategoryType getType();
    BigDecimal getTotalAmount();
    Long getTransactionCount();
}
