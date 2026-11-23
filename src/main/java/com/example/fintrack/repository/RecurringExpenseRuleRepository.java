package com.example.fintrack.repository;

import com.example.fintrack.entity.RecurringExpenseRule;
import com.example.fintrack.entity.RecurringExpenseStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurringExpenseRuleRepository extends JpaRepository<RecurringExpenseRule, Long> {
    List<RecurringExpenseRule> findByUserIdOrderByNextRunDateAsc(Long userId);
    boolean existsByCategory_Id(Long categoryId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RecurringExpenseRule r where r.id = :id and r.user.id = :userId")
    Optional<RecurringExpenseRule> findOwnedForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RecurringExpenseRule r " +
            "where r.status = :status and r.nextRunDate <= :through order by r.nextRunDate, r.id")
    List<RecurringExpenseRule> findDueForUpdate(@Param("status") RecurringExpenseStatus status,
                                                @Param("through") LocalDate through);
}
