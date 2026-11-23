package com.example.fintrack.repository;

import com.example.fintrack.entity.SubscriptionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubscriptionCategoryRepository extends JpaRepository<SubscriptionCategory, Long> {
    List<SubscriptionCategory> findByUserIdOrderByNameAsc(Long userId);
    @Query("select c from SubscriptionCategory c where c.id = :id " +
            "and c.user.id = :userId")
    Optional<SubscriptionCategory> findAvailableToUser(@Param("id") Long id, @Param("userId") Long userId);
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
}
