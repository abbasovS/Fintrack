package com.example.fintrack.service;

import com.example.fintrack.dto.CategoryReportResponse;
import com.example.fintrack.dto.ReportSummaryResponse;
import com.example.fintrack.dto.ReportTrendResponse;
import com.example.fintrack.entity.CategoryType;
import com.example.fintrack.repository.CategoryReportProjection;
import com.example.fintrack.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {
    private final TransactionRepository transactionRepository;

    @Value("${fintrack.currency:AZN}")
    private String currency;

    @Value("${fintrack.time-zone:Asia/Baku}")
    private String timeZone;

    public ReportSummaryResponse summary(Long userId, Instant from, Instant to) {
        BigDecimal income = amountOrZero(transactionRepository.sumByUserAndTypeBetween(userId, CategoryType.INCOME, from, to));
        BigDecimal expense = amountOrZero(transactionRepository.sumByUserAndTypeBetween(userId, CategoryType.EXPENSE, from, to));
        return new ReportSummaryResponse(from, to, currency, income, expense, income.subtract(expense),
                transactionRepository.countByUserBetween(userId, from, to));
    }

    public List<CategoryReportResponse> categories(Long userId, Instant from, Instant to, CategoryType type) {
        return transactionRepository.sumByCategory(userId, from, to, type).stream()
                .map(this::toResponse).toList();
    }

    public List<ReportTrendResponse> trend(Long userId, Instant from, Instant to, String interval) {
        String postgresInterval = switch (interval) {
            case "DAY" -> "day";
            case "WEEK" -> "week";
            case "MONTH" -> "month";
            default -> throw new IllegalArgumentException("interval must be DAY, WEEK, or MONTH");
        };
        ZoneId zone = ZoneId.of(timeZone);
        LocalDate first = from.atZone(zone).toLocalDate();
        LocalDate last = to.minusNanos(1).atZone(zone).toLocalDate();
        if ("WEEK".equals(interval)) first = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if ("MONTH".equals(interval)) first = first.withDayOfMonth(1);
        Map<LocalDate, BigDecimal[]> totals = new TreeMap<>();
        for (Object[] row : transactionRepository.trendByUser(userId, from, to, postgresInterval, timeZone)) {
            LocalDate period = row[0] instanceof java.sql.Date sqlDate ? sqlDate.toLocalDate() : (LocalDate) row[0];
            int amountIndex = "INCOME".equals(row[1]) ? 0 : 1;
            totals.computeIfAbsent(period, ignored -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO})[amountIndex] = (BigDecimal) row[2];
        }
        List<ReportTrendResponse> result = new java.util.ArrayList<>();
        LocalDate current = first;
        while (!current.isAfter(last)) {
            BigDecimal[] amounts = totals.getOrDefault(current, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            result.add(new ReportTrendResponse(current, amounts[0], amounts[1]));
            current = switch (interval) {
                case "WEEK" -> current.plusWeeks(1);
                case "MONTH" -> current.plusMonths(1);
                default -> current.plusDays(1);
            };
        }
        return result;
    }

    private BigDecimal amountOrZero(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private CategoryReportResponse toResponse(CategoryReportProjection projection) {
        return new CategoryReportResponse(projection.getCategoryId(), projection.getCategoryName(),
                projection.getType(), projection.getTotalAmount(), projection.getTransactionCount());
    }
}
