package com.example.fintrack.repository;

import com.example.fintrack.entity.CategoryType;
import com.example.fintrack.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
    boolean existsByCategory_Id(Long categoryId);

    @Query("select sum(t.amount) from Transaction t " +
            "where t.user.id = :userId and t.type = :type and t.createdAt >= :from and t.createdAt < :to")
    BigDecimal sumByUserAndTypeBetween(@Param("userId") Long userId,
                                       @Param("type") CategoryType type,
                                       @Param("from") Instant from,
                                       @Param("to") Instant to);

    @Query("select sum(t.amount) from Transaction t where t.user.id = :userId " +
            "and t.category.id = :categoryId and t.type = :type " +
            "and t.createdAt >= :from and t.createdAt < :to")
    BigDecimal sumByUserCategoryAndTypeBetween(@Param("userId") Long userId,
                                                @Param("categoryId") Long categoryId,
                                                @Param("type") CategoryType type,
                                                @Param("from") Instant from,
                                                @Param("to") Instant to);

    @Query("select count(t) from Transaction t where t.user.id = :userId " +
            "and t.createdAt >= :from and t.createdAt < :to")
    long countByUserBetween(@Param("userId") Long userId,
                            @Param("from") Instant from,
                            @Param("to") Instant to);

    @Query(value = "select cast(date_trunc(:interval, created_at at time zone :zone) as date) as period, " +
            "type as type, sum(amount) as total " +
            "from transactions where user_id = :userId and created_at >= :from and created_at < :to " +
            "group by period, type order by period", nativeQuery = true)
    List<Object[]> trendByUser(@Param("userId") Long userId,
                               @Param("from") Instant from,
                               @Param("to") Instant to,
                               @Param("interval") String interval,
                               @Param("zone") String zone);

    @Query("select c.id as categoryId, c.name as categoryName, t.type as type, " +
            "sum(t.amount) as totalAmount, count(t) as transactionCount " +
            "from Transaction t join t.category c " +
            "where t.user.id = :userId and t.createdAt >= :from and t.createdAt < :to " +
            "and (:type is null or t.type = :type) " +
            "group by c.id, c.name, t.type order by sum(t.amount) desc")
    List<CategoryReportProjection> sumByCategory(@Param("userId") Long userId,
                                                 @Param("from") Instant from,
                                                 @Param("to") Instant to,
                                                 @Param("type") CategoryType type);
}
