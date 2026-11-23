package com.example.fintrack.service.impl;

import com.example.fintrack.dto.RecurringExpenseOccurrenceResponse;
import com.example.fintrack.dto.RecurringExpenseRequest;
import com.example.fintrack.dto.RecurringExpenseResponse;
import com.example.fintrack.entity.*;
import com.example.fintrack.exception.ResourceNotFoundException;
import com.example.fintrack.repository.CategoryRepository;
import com.example.fintrack.repository.RecurringExpenseOccurrenceRepository;
import com.example.fintrack.repository.RecurringExpenseRuleRepository;
import com.example.fintrack.repository.TransactionRepository;
import com.example.fintrack.repository.UserRepository;
import com.example.fintrack.service.RecurringExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RecurringExpenseServiceImpl implements RecurringExpenseService {
    private final RecurringExpenseRuleRepository ruleRepository;
    private final RecurringExpenseOccurrenceRepository occurrenceRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Value("${fintrack.currency:AZN}")
    private String currency;

    @Value("${fintrack.time-zone:Asia/Baku}")
    private String timeZone;

    @Override
    @Transactional(readOnly = true)
    public List<RecurringExpenseResponse> findAll(Long userId) {
        return ruleRepository.findByUserIdOrderByNextRunDateAsc(userId).stream()
                .map(this::response).toList();
    }

    @Override
    public RecurringExpenseResponse create(Long userId, RecurringExpenseRequest request) {
        validateSchedule(request);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Category category = categoryRepository.findByIdAndUserId(request.categoryId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        RecurringExpenseRule rule = RecurringExpenseRule.builder()
                .user(user)
                .category(category)
                .name(request.name().trim())
                .amount(request.amount())
                .frequency(request.frequency())
                .dayOfWeek(request.dayOfWeek())
                .dayOfMonth(request.dayOfMonth())
                .skipWeekends(request.skipWeekends())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .nextRunDate(firstRunDate(request))
                .status(RecurringExpenseStatus.ACTIVE)
                .build();
        return response(ruleRepository.save(rule));
    }

    @Override
    public RecurringExpenseResponse update(Long userId, Long id, RecurringExpenseRequest request) {
        validateSchedule(request);
        RecurringExpenseRule rule = ownedRule(userId, id);
        Category category = categoryRepository.findByIdAndUserId(request.categoryId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (rule.getStatus() == RecurringExpenseStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Completed rule cannot be changed");
        }
        rule.setCategory(category);
        rule.setName(request.name().trim());
        rule.setAmount(request.amount());
        rule.setFrequency(request.frequency());
        rule.setDayOfWeek(request.dayOfWeek());
        rule.setDayOfMonth(request.dayOfMonth());
        rule.setSkipWeekends(request.skipWeekends());
        rule.setStartDate(request.startDate());
        rule.setEndDate(request.endDate());
        rule.setNextRunDate(firstRunDate(request));
        if (rule.getEndDate() != null && rule.getNextRunDate().isAfter(rule.getEndDate())) {
            rule.setStatus(RecurringExpenseStatus.COMPLETED);
        }
        return response(ruleRepository.save(rule));
    }

    @Override
    public RecurringExpenseResponse pause(Long userId, Long id) {
        RecurringExpenseRule rule = ownedRule(userId, id);
        if (rule.getStatus() == RecurringExpenseStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Completed rule cannot be paused");
        }
        rule.setStatus(RecurringExpenseStatus.PAUSED);
        return response(ruleRepository.save(rule));
    }

    @Override
    public RecurringExpenseResponse resume(Long userId, Long id) {
        RecurringExpenseRule rule = ownedRule(userId, id);
        if (rule.getStatus() == RecurringExpenseStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Completed rule cannot be resumed");
        }
        LocalDate today = LocalDate.now(ZoneId.of(timeZone));
        if (rule.getEndDate() != null && rule.getEndDate().isBefore(today)) {
            rule.setStatus(RecurringExpenseStatus.COMPLETED);
            return response(ruleRepository.save(rule));
        }
        while (rule.getNextRunDate().isBefore(today)) {
            rule.setNextRunDate(nextRunDate(rule, rule.getNextRunDate()));
        }
        if (rule.getEndDate() != null && rule.getNextRunDate().isAfter(rule.getEndDate())) {
            rule.setStatus(RecurringExpenseStatus.COMPLETED);
            return response(ruleRepository.save(rule));
        }
        rule.setStatus(RecurringExpenseStatus.ACTIVE);
        return response(ruleRepository.save(rule));
    }

    @Override
    public void delete(Long userId, Long id) {
        ruleRepository.delete(ownedRule(userId, id));
    }

    @Override
    public RecurringExpenseOccurrenceResponse skip(Long userId, Long id, LocalDate date) {
        RecurringExpenseRule rule = ownedRule(userId, id);
        LocalDate today = LocalDate.now(ZoneId.of(timeZone));
        if (date.isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Past dates cannot be skipped");
        }
        if (!isScheduledDate(rule, date)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Selected date is not part of this recurring schedule");
        }
        RecurringExpenseOccurrence occurrence = occurrenceRepository
                .findByRuleIdAndScheduledDate(id, date).orElse(null);
        if (occurrence != null) {
            if (occurrence.getStatus() == RecurringExpenseOccurrenceStatus.SKIPPED) {
                return occurrenceResponse(occurrence);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Expense for this date has already been created");
        }
        occurrence = occurrenceRepository.save(RecurringExpenseOccurrence.builder()
                .rule(rule)
                .scheduledDate(date)
                .status(RecurringExpenseOccurrenceStatus.SKIPPED)
                .build());
        if (date.equals(rule.getNextRunDate())) {
            LocalDate nextDate = nextRunDate(rule, date);
            rule.setNextRunDate(nextDate);
            if (rule.getEndDate() != null && nextDate.isAfter(rule.getEndDate())) {
                rule.setStatus(RecurringExpenseStatus.COMPLETED);
            }
            ruleRepository.save(rule);
        }
        return occurrenceResponse(occurrence);
    }

    @Scheduled(cron = "${fintrack.recurring-expenses.cron:0 55 23 * * *}", zone = "${fintrack.time-zone:Asia/Baku}")
    public void createDueExpenses() {
        processThrough(LocalDate.now(ZoneId.of(timeZone)));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpMissedDays() {
        processThrough(LocalDate.now(ZoneId.of(timeZone)).minusDays(1));
    }

    private void processThrough(LocalDate through) {
        if (through == null) return;
        List<RecurringExpenseRule> dueRules = ruleRepository.findDueForUpdate(RecurringExpenseStatus.ACTIVE, through);
        for (RecurringExpenseRule rule : dueRules) {
            while (!rule.getNextRunDate().isAfter(through)) {
                LocalDate scheduledDate = rule.getNextRunDate();
                if (rule.getEndDate() != null && scheduledDate.isAfter(rule.getEndDate())) {
                    rule.setStatus(RecurringExpenseStatus.COMPLETED);
                    break;
                }

                if (occurrenceRepository.findByRuleIdAndScheduledDate(rule.getId(), scheduledDate).isEmpty()) {
                    Transaction transaction = transactionRepository.save(Transaction.builder()
                            .user(rule.getUser())
                            .category(rule.getCategory())
                            .amount(rule.getAmount())
                            .type(rule.getCategory().getType())
                            .description(rule.getName())
                            .createdAt(scheduledDate.atTime(12, 0).atZone(ZoneId.of(timeZone)).toInstant())
                            .build());
                    occurrenceRepository.save(RecurringExpenseOccurrence.builder()
                            .rule(rule)
                            .scheduledDate(scheduledDate)
                            .status(RecurringExpenseOccurrenceStatus.CREATED)
                            .transaction(transaction)
                            .build());
                }
                LocalDate nextDate = nextRunDate(rule, scheduledDate);
                if (rule.getEndDate() != null && nextDate.isAfter(rule.getEndDate())) {
                    rule.setStatus(RecurringExpenseStatus.COMPLETED);
                    rule.setNextRunDate(nextDate);
                    break;
                }
                rule.setNextRunDate(nextDate);
            }
            ruleRepository.save(rule);
        }
    }

    private void validateSchedule(RecurringExpenseRequest request) {
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must be on or after start date");
        }
        boolean valid = switch (request.frequency()) {
            case DAILY -> request.dayOfWeek() == null && request.dayOfMonth() == null;
            case WEEKLY -> request.dayOfWeek() != null && request.dayOfMonth() == null;
            case MONTHLY -> request.dayOfWeek() == null && request.dayOfMonth() != null;
        };
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide only the schedule fields required by the selected frequency");
        }
        if (request.skipWeekends() && request.frequency() != RecurringExpenseFrequency.DAILY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Weekend skipping applies only to daily schedules");
        }
        if (request.endDate() != null && firstRunDate(request).isAfter(request.endDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The selected date range contains no scheduled expense date");
        }
    }

    private LocalDate firstRunDate(RecurringExpenseRequest request) {
        return switch (request.frequency()) {
            case DAILY -> shiftOffWeekend(request.startDate(), request.skipWeekends());
            case WEEKLY -> request.startDate().with(TemporalAdjusters.nextOrSame(request.dayOfWeek()));
            case MONTHLY -> {
                YearMonth month = YearMonth.from(request.startDate());
                LocalDate candidate = month.atDay(Math.min(request.dayOfMonth(), month.lengthOfMonth()));
                if (candidate.isBefore(request.startDate())) {
                    month = month.plusMonths(1);
                    candidate = month.atDay(Math.min(request.dayOfMonth(), month.lengthOfMonth()));
                }
                yield candidate;
            }
        };
    }

    private LocalDate firstRunDate(RecurringExpenseRule rule) {
        return switch (rule.getFrequency()) {
            case DAILY -> shiftOffWeekend(rule.getStartDate(), rule.isSkipWeekends());
            case WEEKLY -> rule.getStartDate().with(TemporalAdjusters.nextOrSame(rule.getDayOfWeek()));
            case MONTHLY -> {
                YearMonth month = YearMonth.from(rule.getStartDate());
                LocalDate candidate = month.atDay(Math.min(rule.getDayOfMonth(), month.lengthOfMonth()));
                if (candidate.isBefore(rule.getStartDate())) {
                    month = month.plusMonths(1);
                    candidate = month.atDay(Math.min(rule.getDayOfMonth(), month.lengthOfMonth()));
                }
                yield candidate;
            }
        };
    }

    private LocalDate nextRunDate(RecurringExpenseRule rule, LocalDate previousDate) {
        return switch (rule.getFrequency()) {
            case DAILY -> shiftOffWeekend(previousDate.plusDays(1), rule.isSkipWeekends());
            case WEEKLY -> previousDate.plusWeeks(1);
            case MONTHLY -> {
                YearMonth month = YearMonth.from(previousDate).plusMonths(1);
                yield month.atDay(Math.min(rule.getDayOfMonth(), month.lengthOfMonth()));
            }
        };
    }

    private LocalDate shiftOffWeekend(LocalDate date, boolean skipWeekends) {
        if (!skipWeekends) return date;
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }

    private boolean isScheduledDate(RecurringExpenseRule rule, LocalDate date) {
        if (date.isBefore(rule.getStartDate()) || (rule.getEndDate() != null && date.isAfter(rule.getEndDate()))) {
            return false;
        }
        return switch (rule.getFrequency()) {
            case DAILY -> !rule.isSkipWeekends() || (date.getDayOfWeek() != DayOfWeek.SATURDAY
                    && date.getDayOfWeek() != DayOfWeek.SUNDAY);
            case WEEKLY -> date.getDayOfWeek() == rule.getDayOfWeek()
                    && ChronoUnit.DAYS.between(firstRunDate(rule), date) % 7 == 0;
            case MONTHLY -> date.getDayOfMonth() == Math.min(rule.getDayOfMonth(), date.lengthOfMonth())
                    && !date.isBefore(firstRunDate(rule));
        };
    }

    private RecurringExpenseRule ownedRule(Long userId, Long id) {
        return ruleRepository.findOwnedForUpdate(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring expense not found"));
    }

    private RecurringExpenseResponse response(RecurringExpenseRule rule) {
        return new RecurringExpenseResponse(rule.getId(), rule.getName(), rule.getAmount(), currency,
                rule.getCategory().getId(), rule.getCategory().getName(), rule.getCategory().getType(), rule.getFrequency(),
                rule.getDayOfWeek(), rule.getDayOfMonth(), rule.isSkipWeekends(), rule.getStartDate(),
                rule.getEndDate(), rule.getNextRunDate(), rule.getStatus());
    }

    private RecurringExpenseOccurrenceResponse occurrenceResponse(RecurringExpenseOccurrence occurrence) {
        return new RecurringExpenseOccurrenceResponse(occurrence.getId(), occurrence.getRule().getId(),
                occurrence.getScheduledDate(), occurrence.getStatus(),
                occurrence.getTransaction() == null ? null : occurrence.getTransaction().getId());
    }
}
