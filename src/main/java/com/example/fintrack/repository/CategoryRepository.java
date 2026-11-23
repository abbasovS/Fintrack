package com.example.fintrack.repository;

import com.example.fintrack.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserIdOrderByNameAsc(Long userId);
    Optional<Category> findByIdAndUserId(Long id, Long userId);
}
