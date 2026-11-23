package com.example.fintrack.repository;

import com.example.fintrack.entity.Subscription;
import com.example.fintrack.entity.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    @EntityGraph(attributePaths = "category")
    List<Subscription> findByStatus(com.example.fintrack.entity.SubscriptionStatus status);

    @EntityGraph(attributePaths = "category")
    @Query("select s from Subscription s where s.user.id = :userId " +
            "and (:status is null or s.status = :status) " +
            "and (:categoryId is null or s.category.id = :categoryId)")
    Page<Subscription> findForUser(@Param("userId") Long userId,
                                  @Param("status") SubscriptionStatus status,
                                  @Param("categoryId") Long categoryId,
                                  Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Optional<Subscription> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = "category")
    List<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    @Query("select count(s) from Subscription s where s.user.id = :userId and s.status = :status " +
            "and s.nextBillingDate >= :today and s.nextBillingDate <= :until")
    long countUpcoming(@Param("userId") Long userId,
                       @Param("status") SubscriptionStatus status,
                       @Param("today") java.time.LocalDate today,
                       @Param("until") java.time.LocalDate until);

    @Query("select coalesce(sum(s.amount), 0) from Subscription s where s.user.id = :userId " +
            "and s.status = :status and s.nextBillingDate >= :today and s.nextBillingDate <= :until")
    java.math.BigDecimal sumUpcoming(@Param("userId") Long userId,
                                     @Param("status") SubscriptionStatus status,
                                     @Param("today") java.time.LocalDate today,
                                     @Param("until") java.time.LocalDate until);
}
