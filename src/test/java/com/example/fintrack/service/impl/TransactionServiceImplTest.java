package com.example.fintrack.service.impl;

import com.example.fintrack.dto.CreateTransactionRequest;
import com.example.fintrack.dto.TransactionResponse;
import com.example.fintrack.dto.UpdateTransactionRequest;
import com.example.fintrack.entity.Category;
import com.example.fintrack.entity.CategoryType;
import com.example.fintrack.entity.Transaction;
import com.example.fintrack.entity.User;
import com.example.fintrack.entity.SubscriptionOccurrence;
import com.example.fintrack.exception.ResourceNotFoundException;
import com.example.fintrack.mapper.TransactionMapper;
import com.example.fintrack.repository.CategoryRepository;
import com.example.fintrack.repository.SubscriptionOccurrenceRepository;
import com.example.fintrack.repository.TransactionRepository;
import com.example.fintrack.repository.UserRepository;
import com.example.fintrack.service.SubscriptionService;
import com.example.fintrack.service.TransactionFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {
    @Mock private TransactionRepository transactionRepository;
    @Mock private TransactionMapper transactionMapper;
    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private SubscriptionOccurrenceRepository occurrenceRepository;
    @Mock private SubscriptionService subscriptionService;
    @InjectMocks private TransactionServiceImpl transactionService;

    @Test
    void findAll_shouldReturnPagedMappedTransactions() {
        Transaction transaction = transaction(1L, CategoryType.EXPENSE);
        TransactionResponse response = response(1L);
        when(transactionRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<Transaction>>any(),
                any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(transaction), PageRequest.of(0, 20), 1));
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        var result = transactionService.findAll(5L, 0, 20,
                new TransactionFilter(null, null, null, null, null));

        assertEquals(List.of(response), result.items());
        assertEquals(1, result.totalItems());
        assertEquals(1, result.totalPages());
        assertEquals(false, result.hasNext());
    }

    @Test
    void save_shouldRejectCategoryWithDifferentType() {
        User user = User.builder().id(5L).build();
        Category category = Category.builder().id(10L).type(CategoryType.INCOME).build();
        CreateTransactionRequest request = new CreateTransactionRequest(
                10L, BigDecimal.TEN, CategoryType.EXPENSE, "Food");

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(categoryRepository.findByIdAndUserId(10L, 5L)).thenReturn(Optional.of(category));

        assertThrows(ResponseStatusException.class,
                () -> transactionService.save(5L, request));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void save_shouldAttachUserAndCategory() {
        User user = User.builder().id(5L).build();
        Category category = Category.builder().id(10L).type(CategoryType.EXPENSE).build();
        Transaction entity = transaction(null, CategoryType.EXPENSE);
        Transaction saved = transaction(1L, CategoryType.EXPENSE);
        TransactionResponse expected = response(1L);
        CreateTransactionRequest request = new CreateTransactionRequest(
                10L, BigDecimal.TEN, CategoryType.EXPENSE, "Food");

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(categoryRepository.findByIdAndUserId(10L, 5L)).thenReturn(Optional.of(category));
        when(transactionMapper.toEntity(request)).thenReturn(entity);
        when(transactionRepository.save(entity)).thenReturn(saved);
        when(transactionMapper.toResponse(saved)).thenReturn(expected);

        assertEquals(expected, transactionService.save(5L, request));
        assertEquals(user, entity.getUser());
        assertEquals(category, entity.getCategory());
    }

    @Test
    void findById_shouldThrowWhenTransactionDoesNotBelongToUser() {
        when(transactionRepository.findOne(
                org.mockito.ArgumentMatchers.<Specification<Transaction>>any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.findById(5L, 1L));
    }

    @Test
    void delete_shouldReverseSubscriptionPaymentThenDelete() {
        Transaction transaction = transaction(1L, CategoryType.EXPENSE);
        when(transactionRepository.findOne(
                org.mockito.ArgumentMatchers.<Specification<Transaction>>any())).thenReturn(Optional.of(transaction));

        transactionService.delete(5L, 1L);

        verify(subscriptionService).reversePaymentForTransaction(1L);
        verify(transactionRepository).delete(transaction);
    }

    @Test
    void update_shouldTrimDescriptionAndSave() {
        Transaction transaction = transaction(1L, CategoryType.EXPENSE);
        Category category = Category.builder().id(10L).type(CategoryType.EXPENSE).build();
        UpdateTransactionRequest request = new UpdateTransactionRequest(
                10L, BigDecimal.valueOf(25), CategoryType.EXPENSE, "  groceries  ");
        TransactionResponse expected = response(1L);

        when(transactionRepository.findOne(
                org.mockito.ArgumentMatchers.<Specification<Transaction>>any())).thenReturn(Optional.of(transaction));
        when(categoryRepository.findByIdAndUserId(10L, 5L)).thenReturn(Optional.of(category));
        when(occurrenceRepository.findByTransactionId(1L)).thenReturn(Optional.empty());
        when(transactionRepository.save(transaction)).thenReturn(transaction);
        when(transactionMapper.toResponse(transaction)).thenReturn(expected);

        assertEquals(expected, transactionService.update(5L, 1L, request));
        assertEquals("groceries", transaction.getDescription());
        assertEquals(BigDecimal.valueOf(25), transaction.getAmount());
    }

    @Test
    void update_shouldRejectSubscriptionTransactionChangedToIncome() {
        Transaction transaction = transaction(1L, CategoryType.EXPENSE);
        Category category = Category.builder().id(10L).type(CategoryType.INCOME).build();
        UpdateTransactionRequest request = new UpdateTransactionRequest(
                10L, BigDecimal.TEN, CategoryType.INCOME, "Income");

        when(transactionRepository.findOne(
                org.mockito.ArgumentMatchers.<Specification<Transaction>>any())).thenReturn(Optional.of(transaction));
        when(categoryRepository.findByIdAndUserId(10L, 5L)).thenReturn(Optional.of(category));
        when(occurrenceRepository.findByTransactionId(1L))
                .thenReturn(Optional.of(new SubscriptionOccurrence()));

        assertThrows(ResponseStatusException.class,
                () -> transactionService.update(5L, 1L, request));
    }

    private Transaction transaction(Long id, CategoryType type) {
        return Transaction.builder().id(id).amount(BigDecimal.TEN).type(type).build();
    }

    private TransactionResponse response(Long id) {
        return TransactionResponse.builder().id(id).amount(BigDecimal.TEN).type(CategoryType.EXPENSE).build();
    }
}
