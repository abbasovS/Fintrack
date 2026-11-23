package com.example.fintrack.controller;

import com.example.fintrack.dto.CreateTransactionRequest;
import com.example.fintrack.dto.PageResponse;
import com.example.fintrack.dto.TransactionResponse;
import com.example.fintrack.dto.UpdateTransactionRequest;
import com.example.fintrack.entity.CategoryType;
import com.example.fintrack.exception.ResourceNotFoundException;
import com.example.fintrack.service.TransactionFilter;
import com.example.fintrack.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @GetMapping
    public PageResponse<TransactionResponse> findAll(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) CategoryType type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String query) {
        validatePage(page, size);
        validateRange(from, to);
        return transactionService.findAll(userId(jwt), page, size,
                new TransactionFilter(from, to, type, categoryId, query));
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> save(@RequestBody @Valid CreateTransactionRequest request,
                                                     @AuthenticationPrincipal Jwt jwt) {
        TransactionResponse created = transactionService.save(userId(jwt), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        transactionService.delete(userId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public TransactionResponse update(@PathVariable Long id, @RequestBody @Valid UpdateTransactionRequest request,
                                      @AuthenticationPrincipal Jwt jwt) {
        return transactionService.update(userId(jwt), id, request);
    }

    @GetMapping("/{id}")
    public TransactionResponse findById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return transactionService.findById(userId(jwt), id);
    }

    private Long userId(Jwt jwt) {
        return ((Number) jwt.getClaim("uid")).longValue();
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0 and size must be between 1 and 100");
        }
    }

    private void validateRange(Instant from, Instant to) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be before to");
        }
    }
}
