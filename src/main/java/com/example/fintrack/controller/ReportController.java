package com.example.fintrack.controller;

import com.example.fintrack.dto.CategoryReportResponse;
import com.example.fintrack.dto.ReportSummaryResponse;
import com.example.fintrack.dto.ReportTrendResponse;
import com.example.fintrack.entity.CategoryType;
import com.example.fintrack.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/summary")
    public ReportSummaryResponse summary(@AuthenticationPrincipal Jwt jwt,
                                         @RequestParam Instant from,
                                         @RequestParam Instant to) {
        validateRange(from, to);
        return reportService.summary(userId(jwt), from, to);
    }

    @GetMapping("/categories")
    public List<CategoryReportResponse> categories(@AuthenticationPrincipal Jwt jwt,
                                                    @RequestParam Instant from,
                                                    @RequestParam Instant to,
                                                    @RequestParam(required = false) CategoryType type) {
        validateRange(from, to);
        return reportService.categories(userId(jwt), from, to, type);
    }

    @GetMapping("/trend")
    public List<ReportTrendResponse> trend(@AuthenticationPrincipal Jwt jwt,
                                           @RequestParam Instant from,
                                           @RequestParam Instant to,
                                           @RequestParam(defaultValue = "DAY") String interval) {
        validateRange(from, to);
        String normalizedInterval = interval.toUpperCase(java.util.Locale.ROOT);
        if (!List.of("DAY", "WEEK", "MONTH").contains(normalizedInterval)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "interval must be DAY, WEEK, or MONTH");
        }
        return reportService.trend(userId(jwt), from, to, normalizedInterval);
    }

    private Long userId(Jwt jwt) {
        return ((Number) jwt.getClaim("uid")).longValue();
    }

    private void validateRange(Instant from, Instant to) {
        if (!from.isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be before to");
        }
    }
}
