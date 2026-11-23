package com.example.fintrack.controller;

import com.example.fintrack.dto.MonthlyBudgetRequest;
import com.example.fintrack.dto.MonthlyBudgetResponse;
import com.example.fintrack.dto.UpdateMonthlyBudgetRequest;
import com.example.fintrack.service.MonthlyBudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class MonthlyBudgetController {
    private final MonthlyBudgetService service;

    @GetMapping
    public List<MonthlyBudgetResponse> find(@AuthenticationPrincipal Jwt jwt,
                                            @RequestParam YearMonth month) {
        return service.findForMonth(userId(jwt), month);
    }

    @PostMapping
    public MonthlyBudgetResponse create(@AuthenticationPrincipal Jwt jwt,
                                        @Valid @RequestBody MonthlyBudgetRequest request) {
        return service.create(userId(jwt), request);
    }

    @PutMapping("/{id}")
    public MonthlyBudgetResponse update(@PathVariable Long id,
                                        @AuthenticationPrincipal Jwt jwt,
                                        @Valid @RequestBody UpdateMonthlyBudgetRequest request) {
        return service.update(userId(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        service.delete(userId(jwt), id);
    }

    private Long userId(Jwt jwt) {
        return ((Number) jwt.getClaim("uid")).longValue();
    }
}
