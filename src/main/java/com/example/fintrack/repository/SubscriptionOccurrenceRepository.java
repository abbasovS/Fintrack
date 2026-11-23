package com.example.fintrack.repository;

import com.example.fintrack.entity.SubscriptionOccurrence;
import com.example.fintrack.entity.SubscriptionOccurrenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionOccurrenceRepository extends JpaRepository<SubscriptionOccurrence, Long> {
    @Query("select o from SubscriptionOccurrence o join fetch o.subscription " +
            "where o.subscription.id in :subscriptionIds and o.status = :status " +
            "and o.subscription.status = :active and o.dueDate = o.subscription.nextBillingDate")
    List<SubscriptionOccurrence> findCurrentOccurrences(@Param("subscriptionIds") List<Long> subscriptionIds,
                                                        @Param("status") SubscriptionOccurrenceStatus status,
                                                        @Param("active") com.example.fintrack.entity.SubscriptionStatus active);

    @Query("select o from SubscriptionOccurrence o join fetch o.subscription s " +
            "join fetch s.category where o.id = :id and s.id = :subscriptionId and s.user.id = :userId")
    Optional<SubscriptionOccurrence> findOwned(@Param("id") Long id,
                                               @Param("subscriptionId") Long subscriptionId,
                                               @Param("userId") Long userId);

    @Modifying
    @Query(value = "insert into subscription_occurrences " +
            "(subscription_id, due_date, expected_amount, status, created_at, updated_at) " +
            "values (:subscriptionId, :dueDate, :amount, 'PLANNED', now(), now()) " +
            "on conflict (subscription_id, due_date) do nothing", nativeQuery = true)
    void insertPlannedIfMissing(@Param("subscriptionId") Long subscriptionId,
                                @Param("dueDate") LocalDate dueDate,
                                @Param("amount") BigDecimal amount);

    @Modifying
    void deleteBySubscriptionIdAndStatus(Long subscriptionId, SubscriptionOccurrenceStatus status);

    @Query("select count(o) from SubscriptionOccurrence o where o.subscription.user.id = :userId " +
            "and o.subscription.status = :active and o.status = :planned " +
            "and o.dueDate >= :today and o.dueDate <= :until")
    long countUpcoming(@Param("userId") Long userId,
                       @Param("active") com.example.fintrack.entity.SubscriptionStatus active,
                       @Param("planned") SubscriptionOccurrenceStatus planned,
                       @Param("today") LocalDate today,
                       @Param("until") LocalDate until);

    @Query("select coalesce(sum(o.expectedAmount), 0) from SubscriptionOccurrence o " +
            "where o.subscription.user.id = :userId and o.subscription.status = :active " +
            "and o.status = :planned and o.dueDate >= :today and o.dueDate <= :until")
    BigDecimal sumUpcoming(@Param("userId") Long userId,
                           @Param("active") com.example.fintrack.entity.SubscriptionStatus active,
                           @Param("planned") SubscriptionOccurrenceStatus planned,
                           @Param("today") LocalDate today,
                           @Param("until") LocalDate until);

    @Query("select coalesce(sum(o.expectedAmount), 0) from SubscriptionOccurrence o " +
            "where o.subscription.user.id = :userId and o.subscription.status = :active " +
            "and o.status = :planned and o.dueDate >= :from and o.dueDate < :to")
    BigDecimal sumPlannedBetween(@Param("userId") Long userId,
                                 @Param("active") com.example.fintrack.entity.SubscriptionStatus active,
                                 @Param("planned") SubscriptionOccurrenceStatus planned,
                                 @Param("from") LocalDate from,
                                 @Param("to") LocalDate to);

    boolean existsByTransactionId(Long transactionId);

    @Query("select o from SubscriptionOccurrence o join fetch o.subscription " +
            "where o.transaction.id = :transactionId")
    Optional<SubscriptionOccurrence> findByTransactionId(@Param("transactionId") Long transactionId);

    @Query("select count(o) from SubscriptionOccurrence o where o.subscription.id = :subscriptionId " +
            "and o.dueDate > :dueDate and o.status <> :planned")
    long countProcessedAfter(@Param("subscriptionId") Long subscriptionId,
                             @Param("dueDate") LocalDate dueDate,
                             @Param("planned") SubscriptionOccurrenceStatus planned);
}
