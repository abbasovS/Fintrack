package com.example.fintrack.dto;

import com.example.fintrack.entity.CategoryType;
import lombok.Builder;

@Builder
public record CategoryResponse(
        Long id,
        String name,
        CategoryType type
) {
}
