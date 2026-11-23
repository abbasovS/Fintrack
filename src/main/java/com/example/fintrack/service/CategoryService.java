package com.example.fintrack.service;

import com.example.fintrack.dto.CategoryResponse;
import com.example.fintrack.dto.CreateCategoryRequest;
import com.example.fintrack.dto.UpdateCategoryRequest;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> findAll(Long userId);
    CategoryResponse save(Long userId, CreateCategoryRequest category);
    void delete(Long userId, Long id);
    CategoryResponse update(Long userId, Long id, UpdateCategoryRequest category);
    CategoryResponse findById(Long userId, Long id);
}
