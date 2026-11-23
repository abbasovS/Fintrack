package com.example.fintrack.mapper;

import com.example.fintrack.dto.CategoryResponse;
import com.example.fintrack.dto.CreateCategoryRequest;
import com.example.fintrack.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public Category toEntity(CreateCategoryRequest request) {
        if(request == null) {
            return null;
        }
        return Category.builder()
                .name(request.name())
                .type(request.type())
                .build();
    }


    public CategoryResponse toResponse(Category entity) {
        if(entity == null) {
            return null;
        }
        return CategoryResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType())
                .build();
    }

}
