package com.example.fintrack.service.impl;

import com.example.fintrack.dto.CreateTransactionRequest;
import com.example.fintrack.dto.PageResponse;
import com.example.fintrack.dto.TransactionResponse;
import com.example.fintrack.dto.UpdateTransactionRequest;
import com.example.fintrack.entity.Category;
import com.example.fintrack.entity.Transaction;
import com.example.fintrack.entity.User;
import com.example.fintrack.exception.ResourceNotFoundException;
import com.example.fintrack.mapper.TransactionMapper;
import com.example.fintrack.repository.CategoryRepository;
import com.example.fintrack.repository.TransactionRepository;
import com.example.fintrack.repository.SubscriptionOccurrenceRepository;
import com.example.fintrack.repository.UserRepository;
import com.example.fintrack.service.TransactionFilter;
import com.example.fintrack.service.TransactionService;
import com.example.fintrack.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SubscriptionOccurrenceRepository occurrenceRepository;
    private final SubscriptionService subscriptionService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> findAll(Long userId, int page, int size, TransactionFilter filter) {
        Specification<Transaction> specification = (root, query, builder) ->
                builder.equal(root.get("user").get("id"), userId);
        if (filter.from() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("createdAt"), filter.from()));
        }
        if (filter.to() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThan(root.get("createdAt"), filter.to()));
        }
        if (filter.type() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("type"), filter.type()));
        }
        if (filter.categoryId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("category").get("id"), filter.categoryId()));
        }
        if (filter.query() != null && !filter.query().isBlank()) {
            String match = "%" + filter.query().trim().toLowerCase() + "%";
            specification = specification.and((root, query, builder) ->
                    builder.like(builder.lower(root.get("description")), match));
        }
        Page<Transaction> result = transactionRepository.findAll(specification,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))));
        return new PageResponse<>(result.getContent().stream().map(transactionMapper::toResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(),
                result.hasNext(), result.hasPrevious());
    }

    @Override
    public TransactionResponse save(Long userId, CreateTransactionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Category category = findAvailableCategory(request.categoryId(), userId);
        validateCategoryType(category, request.type());
        Transaction entity = transactionMapper.toEntity(request);
        entity.setUser(user);
        entity.setCategory(category);
        return transactionMapper.toResponse(transactionRepository.save(entity));
    }

    @Override
    public void delete(Long userId, Long id) {
        Transaction transaction = findOwnedTransaction(id, userId);
        subscriptionService.reversePaymentForTransaction(id);
        transactionRepository.delete(transaction);
    }

    @Override
    public TransactionResponse findById(Long userId, Long id) {
        return transactionMapper.toResponse(findOwnedTransaction(id, userId));
    }

    @Override
    public TransactionResponse update(Long userId, Long id, UpdateTransactionRequest request) {
        Transaction transaction = findOwnedTransaction(id, userId);
        Category category = findAvailableCategory(request.categoryId(), userId);
        validateCategoryType(category, request.type());
        occurrenceRepository.findByTransactionId(id).ifPresent(occurrence -> {
            if (request.type() != com.example.fintrack.entity.CategoryType.EXPENSE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A subscription payment transaction must remain an expense");
            }
            occurrence.setActualAmount(request.amount());
            occurrenceRepository.save(occurrence);
        });
        transaction.setAmount(request.amount());
        transaction.setDescription(request.description() == null ? null : request.description().trim());
        transaction.setType(request.type());
        transaction.setCategory(category);
        return transactionMapper.toResponse(transactionRepository.save(transaction));
    }

    private Transaction findOwnedTransaction(Long id, Long userId) {
        return transactionRepository.findOne((root, query, builder) -> builder.and(
                        builder.equal(root.get("id"), id),
                        builder.equal(root.get("user").get("id"), userId)))
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
    }

    private Category findAvailableCategory(Long categoryId, Long userId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private void validateCategoryType(Category category, com.example.fintrack.entity.CategoryType transactionType) {
        if (category.getType() != transactionType) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction type must match category type");
        }
    }
}
