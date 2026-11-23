package com.example.fintrack.service;

import com.example.fintrack.dto.CreateTransactionRequest;
import com.example.fintrack.dto.PageResponse;
import com.example.fintrack.dto.TransactionResponse;
import com.example.fintrack.dto.UpdateTransactionRequest;

public interface TransactionService {
    PageResponse<TransactionResponse> findAll(Long userId, int page, int size, TransactionFilter filter);
    TransactionResponse save(Long userId, CreateTransactionRequest transaction);
    void delete(Long userId, Long id);
    TransactionResponse update(Long userId, Long id, UpdateTransactionRequest transaction);
    TransactionResponse findById(Long userId, Long id);
}
