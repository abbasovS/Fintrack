package com.example.fintrack.dto;

import com.example.fintrack.entity.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest (
        @NotBlank(message = "Category name cannot be blank")
        @Size(max = 100, message = "Name cannot exceed 100 characters")
        String name,

        @NotNull(message = "Category type is required")
        CategoryType type

){
}
