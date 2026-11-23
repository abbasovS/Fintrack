package com.example.fintrack.controller;

import com.example.fintrack.dto.RecurringExpenseOccurrenceResponse;
import com.example.fintrack.dto.RecurringExpenseRequest;
import com.example.fintrack.dto.RecurringExpenseResponse;
import com.example.fintrack.dto.SkipRecurringExpenseRequest;
import com.example.fintrack.service.RecurringExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recurring-expenses")
@RequiredArgsConstructor
public class RecurringExpenseController {
    private final RecurringExpenseService recurringExpenseService;

    @GetMapping
    public List<RecurringExpenseResponse> findAll(@AuthenticationPrincipal Jwt jwt) {
        return recurringExpenseService.findAll(userId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringExpenseResponse create(@Valid @RequestBody RecurringExpenseRequest request,
                                           @AuthenticationPrincipal Jwt jwt) {
        return recurringExpenseService.create(userId(jwt), request);
    }

    @PutMapping("/{id}")
    public RecurringExpenseResponse update(@PathVariable Long id,
                                           @Valid @RequestBody RecurringExpenseRequest request,
                                           @AuthenticationPrincipal Jwt jwt) {
        return recurringExpenseService.update(userId(jwt), id, request);
    }

    @PostMapping("/{id}/pause")
    public RecurringExpenseResponse pause(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return recurringExpenseService.pause(userId(jwt), id);
    }

    @PostMapping("/{id}/resume")
    public RecurringExpenseResponse resume(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return recurringExpenseService.resume(userId(jwt), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        recurringExpenseService.delete(userId(jwt), id);
    }

    @PostMapping("/{id}/skip")
    public RecurringExpenseOccurrenceResponse skip(@PathVariable Long id,
                                                   @Valid @RequestBody SkipRecurringExpenseRequest request,
                                                   @AuthenticationPrincipal Jwt jwt) {
        return recurringExpenseService.skip(userId(jwt), id, request.date());
    }

    private Long userId(Jwt jwt) {
        return ((Number) jwt.getClaim("uid")).longValue();
    }
}
