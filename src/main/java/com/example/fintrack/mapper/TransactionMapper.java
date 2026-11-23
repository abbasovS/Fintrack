package com.example.fintrack.mapper;

import com.example.fintrack.dto.CreateTransactionRequest;
import com.example.fintrack.dto.TransactionResponse;
import com.example.fintrack.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public Transaction toEntity(CreateTransactionRequest request) {
        if(request == null) {
            return null;
        }
        return Transaction.builder()
                .amount(request.amount())
                .description(request.description())
                .type(request.type())
                .build();

    }

    public TransactionResponse toResponse(Transaction entity) {
        if(entity == null) {
            return null;
        }

        return TransactionResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser() == null ? null : entity.getUser().getId())
                .categoryId(entity.getCategory() == null ? null : entity.getCategory().getId())
                .categoryName(entity.getCategory() == null ? null : entity.getCategory().getName())
                .amount(entity.getAmount())
                .description(entity.getDescription())
                .type(entity.getType())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
