package com.example.fintrack.service;

import com.example.fintrack.dto.MonthlyBudgetRequest;
import com.example.fintrack.dto.MonthlyBudgetResponse;
import com.example.fintrack.dto.UpdateMonthlyBudgetRequest;
import com.example.fintrack.entity.CategoryType;
import com.example.fintrack.entity.MonthlyBudget;
import com.example.fintrack.entity.User;
import com.example.fintrack.exception.ResourceNotFoundException;
import com.example.fintrack.repository.CategoryRepository;
import com.example.fintrack.repository.MonthlyBudgetRepository;
import com.example.fintrack.repository.TransactionRepository;
import com.example.fintrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MonthlyBudgetService {
    private final MonthlyBudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Value("${fintrack.currency:AZN}")
    private String currency;

    @Value("${fintrack.time-zone:Asia/Baku}")
    private String timeZone;

    @Transactional(readOnly = true)
    public List<MonthlyBudgetResponse> findForMonth(Long userId, YearMonth month) {
        return budgetRepository.findByUserIdAndMonthStartOrderByCategoryNameAsc(userId, month.atDay(1))
                .stream().map(this::response).toList();
    }

    public MonthlyBudgetResponse create(Long userId, MonthlyBudgetRequest request) {
        var category = categoryRepository.findByIdAndUserId(request.categoryId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (category.getType() != CategoryType.EXPENSE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Budgets require an expense category");
        }
        LocalDate monthStart = request.month().atDay(1);
        if (budgetRepository.existsByUserIdAndCategoryIdAndMonthStart(userId, category.getId(), monthStart)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A budget already exists for this category and month");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return response(budgetRepository.save(MonthlyBudget.builder().user(user).category(category)
                .monthStart(monthStart).amount(request.amount()).build()));
    }

    public MonthlyBudgetResponse update(Long userId, Long id, UpdateMonthlyBudgetRequest request) {
        MonthlyBudget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        budget.setAmount(request.amount());
        return response(budgetRepository.save(budget));
    }

    public void delete(Long userId, Long id) {
        MonthlyBudget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        budgetRepository.delete(budget);
    }

    private MonthlyBudgetResponse response(MonthlyBudget budget) {
        YearMonth month = YearMonth.from(budget.getMonthStart());
        LocalDate nextMonth = month.plusMonths(1).atDay(1);
        var zone = ZoneId.of(timeZone);
        BigDecimal actual = transactionRepository.sumByUserCategoryAndTypeBetween(budget.getUser().getId(),
                budget.getCategory().getId(), CategoryType.EXPENSE,
                month.atDay(1).atStartOfDay(zone).toInstant(), nextMonth.atStartOfDay(zone).toInstant());
        if (actual == null) actual = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal remaining = budget.getAmount().subtract(actual);
        int percent = actual.multiply(BigDecimal.valueOf(100)).divide(budget.getAmount(), 0, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(Integer.MAX_VALUE)).intValue();
        return new MonthlyBudgetResponse(budget.getId(), budget.getCategory().getId(), budget.getCategory().getName(),
                budget.getMonthStart(), budget.getAmount(), actual, remaining, percent, currency);
    }
}
