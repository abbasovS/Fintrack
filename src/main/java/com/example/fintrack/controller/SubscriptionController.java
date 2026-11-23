package com.example.fintrack.controller;

import com.example.fintrack.dto.*;
import com.example.fintrack.entity.SubscriptionStatus;
import com.example.fintrack.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    @GetMapping("/categories")
    public java.util.List<SubscriptionCategoryResponse> categories(@AuthenticationPrincipal Jwt jwt) {
        return subscriptionService.categories(userId(jwt));
    }

    @GetMapping("/categories/{id}")
    public SubscriptionCategoryResponse findCategory(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return subscriptionService.findCategory(userId(jwt), id);
    }

    @PostMapping("/categories")
    public ResponseEntity<SubscriptionCategoryResponse> createCategory(
            @RequestBody @Valid CreateSubscriptionCategoryRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        SubscriptionCategoryResponse created = subscriptionService.createCategory(userId(jwt), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/summary")
    public SubscriptionSummaryResponse summary(@AuthenticationPrincipal Jwt jwt) {
        return subscriptionService.summary(userId(jwt));
    }

    @GetMapping
    public PageResponse<SubscriptionResponse> findAll(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) SubscriptionStatus status,
            @RequestParam(required = false) Long categoryId) {
        return subscriptionService.findAll(userId(jwt), page, size, status, categoryId);
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> create(@RequestBody @Valid SubscriptionRequest request,
                                                        @AuthenticationPrincipal Jwt jwt) {
        SubscriptionResponse created = subscriptionService.create(userId(jwt), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public SubscriptionResponse findById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return subscriptionService.findById(userId(jwt), id);
    }

    @PutMapping("/{id}")
    public SubscriptionResponse update(@PathVariable Long id, @RequestBody @Valid SubscriptionRequest request,
                                       @AuthenticationPrincipal Jwt jwt) {
        return subscriptionService.update(userId(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        subscriptionService.delete(userId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{subscriptionId}/occurrences/{occurrenceId}/pay")
    public SubscriptionOccurrenceResponse markPaid(@PathVariable Long subscriptionId,
                                                   @PathVariable Long occurrenceId,
                                                   @RequestBody @Valid MarkSubscriptionPaidRequest request,
                                                   @AuthenticationPrincipal Jwt jwt) {
        return subscriptionService.markPaid(userId(jwt), subscriptionId, occurrenceId, request);
    }

    @PostMapping("/{subscriptionId}/occurrences/{occurrenceId}/skip")
    public SubscriptionOccurrenceResponse skipOccurrence(@PathVariable Long subscriptionId,
                                                         @PathVariable Long occurrenceId,
                                                         @AuthenticationPrincipal Jwt jwt) {
        return subscriptionService.skipOccurrence(userId(jwt), subscriptionId, occurrenceId);
    }

    private Long userId(Jwt jwt) {
        return ((Number) jwt.getClaim("uid")).longValue();
    }
}
