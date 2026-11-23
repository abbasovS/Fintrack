package com.example.fintrack.service;

import com.example.fintrack.dto.CreateSubscriptionCategoryRequest;
import com.example.fintrack.dto.PageResponse;
import com.example.fintrack.dto.SubscriptionCategoryResponse;
import com.example.fintrack.dto.SubscriptionRequest;
import com.example.fintrack.dto.SubscriptionResponse;
import com.example.fintrack.dto.SubscriptionSummaryResponse;
import com.example.fintrack.dto.MarkSubscriptionPaidRequest;
import com.example.fintrack.dto.SubscriptionOccurrenceResponse;
import com.example.fintrack.entity.SubscriptionStatus;

import java.util.List;

public interface SubscriptionService {
    List<SubscriptionCategoryResponse> categories(Long userId);
    SubscriptionCategoryResponse findCategory(Long userId, Long id);
    SubscriptionCategoryResponse createCategory(Long userId, CreateSubscriptionCategoryRequest request);
    PageResponse<SubscriptionResponse> findAll(Long userId, int page, int size,
                                                SubscriptionStatus status, Long categoryId);
    SubscriptionResponse findById(Long userId, Long id);
    SubscriptionResponse create(Long userId, SubscriptionRequest request);
    SubscriptionResponse update(Long userId, Long id, SubscriptionRequest request);
    void delete(Long userId, Long id);
    SubscriptionSummaryResponse summary(Long userId);
    SubscriptionOccurrenceResponse markPaid(Long userId, Long subscriptionId, Long occurrenceId,
                                            MarkSubscriptionPaidRequest request);
    SubscriptionOccurrenceResponse skipOccurrence(Long userId, Long subscriptionId, Long occurrenceId);
    void reversePaymentForTransaction(Long transactionId);
}
