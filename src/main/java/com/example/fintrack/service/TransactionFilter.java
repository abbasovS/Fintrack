package com.example.fintrack.service;

import com.example.fintrack.entity.CategoryType;

import java.time.Instant;

public record TransactionFilter(
        Instant from,
        Instant to,
        CategoryType type,
        Long categoryId,
        String query
) { }
