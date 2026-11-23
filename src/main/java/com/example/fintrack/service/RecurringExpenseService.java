package com.example.fintrack.service;

import com.example.fintrack.dto.RecurringExpenseOccurrenceResponse;
import com.example.fintrack.dto.RecurringExpenseRequest;
import com.example.fintrack.dto.RecurringExpenseResponse;

import java.time.LocalDate;
import java.util.List;

public interface RecurringExpenseService {
    List<RecurringExpenseResponse> findAll(Long userId);
    RecurringExpenseResponse create(Long userId, RecurringExpenseRequest request);
    RecurringExpenseResponse update(Long userId, Long id, RecurringExpenseRequest request);
    RecurringExpenseResponse pause(Long userId, Long id);
    RecurringExpenseResponse resume(Long userId, Long id);
    void delete(Long userId, Long id);
    RecurringExpenseOccurrenceResponse skip(Long userId, Long id, LocalDate date);
}
