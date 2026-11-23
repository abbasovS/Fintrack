package com.example.fintrack.repository;

import com.example.fintrack.entity.MonthlyBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MonthlyBudgetRepository extends JpaRepository<MonthlyBudget, Long> {
    List<MonthlyBudget> findByUserIdAndMonthStartOrderByCategoryNameAsc(Long userId, LocalDate monthStart);
    Optional<MonthlyBudget> findByIdAndUserId(Long id, Long userId);
    boolean existsByCategory_Id(Long categoryId);
    boolean existsByUserIdAndCategoryIdAndMonthStart(Long userId, Long categoryId, LocalDate monthStart);
}
