package com.example.fintrack.service.impl;

import com.example.fintrack.dto.CategoryResponse;
import com.example.fintrack.dto.CreateCategoryRequest;
import com.example.fintrack.dto.UpdateCategoryRequest;
import com.example.fintrack.entity.Category;
import com.example.fintrack.entity.User;
import com.example.fintrack.exception.ResourceNotFoundException;
import com.example.fintrack.mapper.CategoryMapper;
import com.example.fintrack.repository.CategoryRepository;
import com.example.fintrack.repository.TransactionRepository;
import com.example.fintrack.repository.UserRepository;
import com.example.fintrack.repository.MonthlyBudgetRepository;
import com.example.fintrack.repository.RecurringExpenseRuleRepository;
import com.example.fintrack.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final MonthlyBudgetRepository budgetRepository;
    private final RecurringExpenseRuleRepository recurringRuleRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll(Long userId) {
        return categoryRepository.findByUserIdOrderByNameAsc(userId)
                .stream().map(categoryMapper::toResponse).toList();
    }

    @Override
    public CategoryResponse save(Long userId, CreateCategoryRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Category category = categoryMapper.toEntity(request);
        category.setUser(user);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    public void delete(Long userId, Long id) {
        Category category = categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (transactionRepository.existsByCategory_Id(id) || budgetRepository.existsByCategory_Id(id)
                || recurringRuleRepository.existsByCategory_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This category is used by transactions, budgets, or recurring rules. Remove those links first.");
        }
        categoryRepository.delete(category);
    }

    @Override
    public CategoryResponse update(Long userId, Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (category.getType() != request.type() && (transactionRepository.existsByCategory_Id(id)
                || recurringRuleRepository.existsByCategory_Id(id))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Category type cannot change while transactions or recurring rules use it");
        }
        category.setName(request.name().trim());
        category.setType(request.type());
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse findById(Long userId, Long id) {
        Category category = categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        return categoryMapper.toResponse(category);
    }
}
