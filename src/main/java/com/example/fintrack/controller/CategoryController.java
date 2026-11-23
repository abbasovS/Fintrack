package com.example.fintrack.controller;

import com.example.fintrack.dto.CategoryResponse;
import com.example.fintrack.dto.CreateCategoryRequest;
import com.example.fintrack.dto.UpdateCategoryRequest;
import com.example.fintrack.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryResponse> findAll(@AuthenticationPrincipal Jwt jwt) {
        return categoryService.findAll(userId(jwt));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> save(@RequestBody @Valid CreateCategoryRequest request,
                                                  @AuthenticationPrincipal Jwt jwt) {
        CategoryResponse created = categoryService.save(userId(jwt), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        categoryService.delete(userId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id, @RequestBody @Valid UpdateCategoryRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        return categoryService.update(userId(jwt), id, request);
    }

    @GetMapping("/{id}")
    public CategoryResponse findById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return categoryService.findById(userId(jwt), id);
    }

    private Long userId(Jwt jwt) {
        return ((Number) jwt.getClaim("uid")).longValue();
    }
}
