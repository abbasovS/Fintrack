package com.example.fintrack.service.impl;

import com.example.fintrack.dto.CategoryResponse;
import com.example.fintrack.dto.CreateCategoryRequest;
import com.example.fintrack.dto.UpdateCategoryRequest;
import com.example.fintrack.entity.Category;
import com.example.fintrack.entity.CategoryType;
import com.example.fintrack.entity.User;
import com.example.fintrack.exception.ResourceNotFoundException;
import com.example.fintrack.mapper.CategoryMapper;
import com.example.fintrack.repository.CategoryRepository;
import com.example.fintrack.repository.MonthlyBudgetRepository;
import com.example.fintrack.repository.RecurringExpenseRuleRepository;
import com.example.fintrack.repository.TransactionRepository;
import com.example.fintrack.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private MonthlyBudgetRepository budgetRepository;

    @Mock
    private RecurringExpenseRuleRepository recurringRuleRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void findAll_shouldReturnMappedCategories() {
        Long userId = 1L;
        Category food = category(10L, "Food", CategoryType.EXPENSE);
        CategoryResponse response = response(10L, "Food", CategoryType.EXPENSE);

        when(categoryRepository.findByUserIdOrderByNameAsc(userId))
                .thenReturn(List.of(food));
        when(categoryMapper.toResponse(food)).thenReturn(response);

        List<CategoryResponse> result = categoryService.findAll(userId);

        assertEquals(List.of(response), result);
        verify(categoryRepository).findByUserIdOrderByNameAsc(userId);
        verify(categoryMapper).toResponse(food);
    }

    @Test
    void findById_shouldReturnCategoryResponse() {
        Long userId = 1L;
        Long categoryId = 10L;
        Category category = category(categoryId, "Food", CategoryType.EXPENSE);
        CategoryResponse expected = response(categoryId, "Food", CategoryType.EXPENSE);

        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(expected);

        CategoryResponse result = categoryService.findById(userId, categoryId);

        assertEquals(expected, result);
        verify(categoryMapper).toResponse(category);
    }

    @Test
    void findById_shouldThrowWhenCategoryDoesNotExist() {
        when(categoryRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.findById(1L, 10L));

        verify(categoryMapper, never()).toResponse(any());
    }

    @Test
    void save_shouldCreateCategoryForExistingUser() {
        Long userId = 1L;
        User user = User.builder().id(userId).build();
        CreateCategoryRequest request = new CreateCategoryRequest("Food", CategoryType.EXPENSE);
        Category entity = category(null, "Food", CategoryType.EXPENSE);
        Category saved = category(10L, "Food", CategoryType.EXPENSE);
        CategoryResponse expected = response(10L, "Food", CategoryType.EXPENSE);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryMapper.toEntity(request)).thenReturn(entity);
        when(categoryRepository.save(entity)).thenReturn(saved);
        when(categoryMapper.toResponse(saved)).thenReturn(expected);

        CategoryResponse result = categoryService.save(userId, request);

        assertEquals(expected, result);
        assertEquals(user, entity.getUser());
        verify(categoryRepository).save(entity);
    }

    @Test
    void save_shouldThrowWhenUserDoesNotExist() {
        CreateCategoryRequest request = new CreateCategoryRequest("Food", CategoryType.EXPENSE);

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.save(1L, request));

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void delete_shouldDeleteUnusedCategory() {
        Category category = category(10L, "Food", CategoryType.EXPENSE);

        when(categoryRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(category));
        when(transactionRepository.existsByCategory_Id(10L)).thenReturn(false);
        when(budgetRepository.existsByCategory_Id(10L)).thenReturn(false);
        when(recurringRuleRepository.existsByCategory_Id(10L)).thenReturn(false);

        categoryService.delete(1L, 10L);

        verify(categoryRepository).delete(category);
    }

    @Test
    void delete_shouldThrowWhenCategoryDoesNotExist() {
        when(categoryRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.delete(1L, 10L));

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void delete_shouldRejectCategoryUsedByTransaction() {
        Category category = category(10L, "Food", CategoryType.EXPENSE);

        when(categoryRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(category));
        when(transactionRepository.existsByCategory_Id(10L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> categoryService.delete(1L, 10L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void update_shouldTrimNameAndUpdateCategory() {
        Category category = category(10L, "Old", CategoryType.EXPENSE);
        UpdateCategoryRequest request = new UpdateCategoryRequest("  Salary  ", CategoryType.INCOME);
        CategoryResponse expected = response(10L, "Salary", CategoryType.INCOME);

        when(categoryRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(category));
        when(transactionRepository.existsByCategory_Id(10L)).thenReturn(false);
        when(recurringRuleRepository.existsByCategory_Id(10L)).thenReturn(false);
        when(categoryRepository.save(category)).thenReturn(category);
        when(categoryMapper.toResponse(category)).thenReturn(expected);

        CategoryResponse result = categoryService.update(1L, 10L, request);

        assertEquals("Salary", category.getName());
        assertEquals(CategoryType.INCOME, category.getType());
        assertEquals(expected, result);
        verify(categoryRepository).save(category);
    }

    @Test
    void update_shouldRejectTypeChangeWhenCategoryHasTransactions() {
        Category category = category(10L, "Food", CategoryType.EXPENSE);
        UpdateCategoryRequest request = new UpdateCategoryRequest("Food", CategoryType.INCOME);

        when(categoryRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(category));
        when(transactionRepository.existsByCategory_Id(10L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> categoryService.update(1L, 10L, request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_shouldThrowWhenCategoryDoesNotExist() {
        UpdateCategoryRequest request = new UpdateCategoryRequest("Food", CategoryType.EXPENSE);

        when(categoryRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.update(1L, 10L, request));

        verify(categoryRepository, never()).save(any());
    }

    private Category category(Long id, String name, CategoryType type) {
        return Category.builder()
                .id(id)
                .name(name)
                .type(type)
                .build();
    }

    private CategoryResponse response(Long id, String name, CategoryType type) {
        return CategoryResponse.builder()
                .id(id)
                .name(name)
                .type(type)
                .build();
    }
}
