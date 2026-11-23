package com.example.fintrack.service.impl;

import com.example.fintrack.dto.*;
import com.example.fintrack.entity.*;
import com.example.fintrack.exception.ResourceNotFoundException;
import com.example.fintrack.repository.SubscriptionCategoryRepository;
import com.example.fintrack.repository.SubscriptionOccurrenceRepository;
import com.example.fintrack.repository.SubscriptionRepository;
import com.example.fintrack.repository.CategoryRepository;
import com.example.fintrack.repository.TransactionRepository;
import com.example.fintrack.repository.UserRepository;
import com.example.fintrack.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.YearMonth;
import java.time.Instant;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionServiceImpl implements SubscriptionService {
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);
    private static final BigDecimal FIFTY_TWO = BigDecimal.valueOf(52);

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionCategoryRepository categoryRepository;
    private final SubscriptionOccurrenceRepository occurrenceRepository;
    private final CategoryRepository expenseCategoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Value("${fintrack.currency:AZN}")
    private String currency;

    @Value("${fintrack.time-zone:Asia/Baku}")
    private String timeZone;

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionCategoryResponse> categories(Long userId) {
        return categoryRepository.findByUserIdOrderByNameAsc(userId).stream()
                .map(this::categoryResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionCategoryResponse findCategory(Long userId, Long id) {
        return categoryRepository.findAvailableToUser(id, userId).map(this::categoryResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription category not found"));
    }

    @Override
    public SubscriptionCategoryResponse createCategory(Long userId, CreateSubscriptionCategoryRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String name = request.name().trim();
        if (categoryRepository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Subscription category already exists");
        }
        SubscriptionCategory category = SubscriptionCategory.builder().name(name).user(user).build();
        return categoryResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubscriptionResponse> findAll(Long userId, int page, int size,
                                                       SubscriptionStatus status, Long categoryId) {
        validatePage(page, size);
        if (categoryId != null && categoryRepository.findAvailableToUser(categoryId, userId).isEmpty()) {
            throw new ResourceNotFoundException("Subscription category not found");
        }
        Page<Subscription> result = subscriptionRepository.findForUser(userId, status, categoryId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "nextBillingDate")
                        .and(Sort.by(Sort.Direction.DESC, "id"))));
        List<Subscription> subscriptions = result.getContent();
        Map<Long, Long> currentOccurrenceIds = currentOccurrenceIds(subscriptions);
        return new PageResponse<>(subscriptions.stream()
                .map(item -> response(item, currentOccurrenceIds.get(item.getId()))).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(),
                result.hasNext(), result.hasPrevious());
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse findById(Long userId, Long id) {
        Subscription subscription = subscriptionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        return response(subscription, currentOccurrenceIds(List.of(subscription)).get(subscription.getId()));
    }

    @Override
    public SubscriptionResponse create(Long userId, SubscriptionRequest request) {
        if (request.status() == SubscriptionStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Create the subscription as active or paused, then cancel it when needed");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        SubscriptionCategory category = categoryRepository.findAvailableToUser(request.categoryId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription category not found"));
        Subscription subscription = Subscription.builder()
                .user(user)
                .category(category)
                .name(request.name().trim())
                .amount(request.amount())
                .billingInterval(request.billingInterval())
                .nextBillingDate(request.nextBillingDate())
                .billingAnchorDate(request.nextBillingDate())
                .status(request.status())
                .note(normalizeNote(request.note()))
                .build();
        subscription = subscriptionRepository.save(subscription);
        generateOccurrences(subscription);
        return response(subscription, currentOccurrenceIds(List.of(subscription)).get(subscription.getId()));
    }

    @Override
    public SubscriptionResponse update(Long userId, Long id, SubscriptionRequest request) {
        Subscription subscription = subscriptionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED
                && request.status() != SubscriptionStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cancelled subscriptions cannot be reactivated; create a new subscription instead");
        }
        SubscriptionCategory category = categoryRepository.findAvailableToUser(request.categoryId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription category not found"));
        boolean scheduleChanged = !subscription.getNextBillingDate().equals(request.nextBillingDate())
                || subscription.getBillingInterval() != request.billingInterval()
                || subscription.getAmount().compareTo(request.amount()) != 0;
        if (scheduleChanged) {
            occurrenceRepository.deleteBySubscriptionIdAndStatus(id, SubscriptionOccurrenceStatus.PLANNED);
            subscription.setBillingAnchorDate(request.nextBillingDate());
        }
        subscription.setCategory(category);
        subscription.setName(request.name().trim());
        subscription.setAmount(request.amount());
        subscription.setBillingInterval(request.billingInterval());
        subscription.setNextBillingDate(request.nextBillingDate());
        subscription.setStatus(request.status());
        subscription.setNote(normalizeNote(request.note()));
        subscription = subscriptionRepository.save(subscription);
        if (request.status() == SubscriptionStatus.CANCELLED) {
            occurrenceRepository.deleteBySubscriptionIdAndStatus(id, SubscriptionOccurrenceStatus.PLANNED);
        } else {
            generateOccurrences(subscription);
        }
        return response(subscription, currentOccurrenceIds(List.of(subscription)).get(subscription.getId()));
    }

    @Override
    public void delete(Long userId, Long id) {
        Subscription subscription = subscriptionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        subscriptionRepository.delete(subscription);
    }

    @Override
    public SubscriptionSummaryResponse summary(Long userId) {
        List<Subscription> active = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);
        BigDecimal monthly = active.stream().map(item -> monthlyEquivalent(item.getAmount(), item.getBillingInterval()))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        LocalDate today = LocalDate.now(ZoneId.of(timeZone));
        LocalDate until = today.plusDays(30);
        long upcomingCount = occurrenceRepository.countUpcoming(userId, SubscriptionStatus.ACTIVE,
                SubscriptionOccurrenceStatus.PLANNED, today, until);
        BigDecimal upcomingAmount = occurrenceRepository.sumUpcoming(userId, SubscriptionStatus.ACTIVE,
                SubscriptionOccurrenceStatus.PLANNED, today, until).setScale(2, RoundingMode.HALF_UP);

        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate nextMonth = monthStart.plusMonths(1);
        Instant rangeFrom = monthStart.atStartOfDay(ZoneId.of(timeZone)).toInstant();
        Instant rangeTo = nextMonth.atStartOfDay(ZoneId.of(timeZone)).toInstant();
        BigDecimal actualIncome = zeroIfNull(transactionRepository.sumByUserAndTypeBetween(
                userId, CategoryType.INCOME, rangeFrom, rangeTo));
        BigDecimal actualExpense = zeroIfNull(transactionRepository.sumByUserAndTypeBetween(
                userId, CategoryType.EXPENSE, rangeFrom, rangeTo));
        BigDecimal plannedExpenses = occurrenceRepository.sumPlannedBetween(userId, SubscriptionStatus.ACTIVE,
                SubscriptionOccurrenceStatus.PLANNED, monthStart, nextMonth).setScale(2, RoundingMode.HALF_UP);
        BigDecimal available = actualIncome.subtract(actualExpense).subtract(plannedExpenses)
                .setScale(2, RoundingMode.HALF_UP);
        return new SubscriptionSummaryResponse(currency, active.size(), monthly, upcomingCount, upcomingAmount,
                monthStart, nextMonth.minusDays(1), actualIncome, actualExpense, plannedExpenses, available);
    }

    @Override
    public SubscriptionOccurrenceResponse markPaid(Long userId, Long subscriptionId, Long occurrenceId,
                                                   MarkSubscriptionPaidRequest request) {
        SubscriptionOccurrence occurrence = ownedOccurrence(userId, subscriptionId, occurrenceId);
        Subscription subscription = occurrence.getSubscription();
        validateCurrentOccurrence(subscription, occurrence);
        Category expenseCategory = findExpenseCategory(request.expenseCategoryId(), userId);

        String description = "Abunəlik ödənişi: " + subscription.getName();
        if (request.note() != null && !request.note().isBlank()) {
            description = (description + " · " + request.note().trim());
        }
        Transaction transaction = Transaction.builder()
                .user(subscription.getUser())
                .category(expenseCategory)
                .amount(request.actualAmount())
                .type(CategoryType.EXPENSE)
                .description(description.substring(0, Math.min(description.length(), 500)))
                .createdAt(request.paidOn().atStartOfDay(ZoneId.of(timeZone)).toInstant())
                .build();
        transaction = transactionRepository.save(transaction);

        occurrence.setStatus(SubscriptionOccurrenceStatus.PAID);
        occurrence.setActualAmount(request.actualAmount());
        occurrence.setPaidOn(request.paidOn());
        occurrence.setTransaction(transaction);
        advanceSchedule(subscription, occurrence.getDueDate());
        occurrenceRepository.save(occurrence);
        subscriptionRepository.save(subscription);
        generateOccurrences(subscription);
        return occurrenceResponse(occurrence);
    }

    @Override
    public SubscriptionOccurrenceResponse skipOccurrence(Long userId, Long subscriptionId, Long occurrenceId) {
        SubscriptionOccurrence occurrence = ownedOccurrence(userId, subscriptionId, occurrenceId);
        Subscription subscription = occurrence.getSubscription();
        validateCurrentOccurrence(subscription, occurrence);
        occurrence.setStatus(SubscriptionOccurrenceStatus.SKIPPED);
        advanceSchedule(subscription, occurrence.getDueDate());
        occurrenceRepository.save(occurrence);
        subscriptionRepository.save(subscription);
        generateOccurrences(subscription);
        return occurrenceResponse(occurrence);
    }

    @Override
    public void reversePaymentForTransaction(Long transactionId) {
        SubscriptionOccurrence occurrence = occurrenceRepository.findByTransactionId(transactionId).orElse(null);
        if (occurrence == null) return;
        Subscription subscription = occurrence.getSubscription();
        if (occurrenceRepository.countProcessedAfter(subscription.getId(), occurrence.getDueDate(),
                SubscriptionOccurrenceStatus.PLANNED) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A later subscription billing cycle has already been processed");
        }
        occurrenceRepository.deleteBySubscriptionIdAndStatus(subscription.getId(), SubscriptionOccurrenceStatus.PLANNED);
        occurrence.setStatus(SubscriptionOccurrenceStatus.PLANNED);
        occurrence.setActualAmount(null);
        occurrence.setPaidOn(null);
        occurrence.setTransaction(null);
        subscription.setNextBillingDate(occurrence.getDueDate());
        occurrenceRepository.saveAndFlush(occurrence);
        subscriptionRepository.save(subscription);
        generateOccurrences(subscription);
    }

    @Scheduled(cron = "0 0 2 * * *", zone = "${fintrack.time-zone:Asia/Baku}")
    public void extendActiveForecasts() {
        subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE).forEach(this::generateOccurrences);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeForecasts() {
        extendActiveForecasts();
    }

    private SubscriptionResponse response(Subscription subscription, Long nextOccurrenceId) {
        return new SubscriptionResponse(subscription.getId(), subscription.getCategory().getId(),
                subscription.getCategory().getName(), subscription.getName(), subscription.getAmount(), currency,
                subscription.getBillingInterval(), subscription.getNextBillingDate(), subscription.getStatus(),
                subscription.getNote(), monthlyEquivalent(subscription.getAmount(), subscription.getBillingInterval()),
                nextOccurrenceId, subscription.getCreatedAt(), subscription.getUpdatedAt());
    }

    private Map<Long, Long> currentOccurrenceIds(List<Subscription> subscriptions) {
        if (subscriptions.isEmpty()) return Map.of();
        List<Long> ids = subscriptions.stream().map(Subscription::getId).toList();
        Map<Long, Long> result = new HashMap<>();
        occurrenceRepository.findCurrentOccurrences(ids, SubscriptionOccurrenceStatus.PLANNED, SubscriptionStatus.ACTIVE)
                .forEach(item -> result.put(item.getSubscription().getId(), item.getId()));
        return result;
    }

    private void generateOccurrences(Subscription subscription) {
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) return;
        LocalDate date = subscription.getNextBillingDate();
        LocalDate horizon = LocalDate.now(ZoneId.of(timeZone)).plusMonths(12);
        int generated = 0;
        while (!date.isAfter(horizon) && generated < 120) {
            occurrenceRepository.insertPlannedIfMissing(subscription.getId(), date, subscription.getAmount());
            date = nextDate(subscription, date);
            generated++;
        }
    }

    private LocalDate nextDate(Subscription subscription, LocalDate currentDueDate) {
        return switch (subscription.getBillingInterval()) {
            case WEEKLY -> currentDueDate.plusWeeks(1);
            case MONTHLY -> monthAtAnchorDay(currentDueDate, subscription.getBillingAnchorDate(), 1);
            case QUARTERLY -> monthAtAnchorDay(currentDueDate, subscription.getBillingAnchorDate(), 3);
            case YEARLY -> monthAtAnchorDay(currentDueDate, subscription.getBillingAnchorDate(), 12);
        };
    }

    private LocalDate monthAtAnchorDay(LocalDate from, LocalDate anchor, int months) {
        YearMonth target = YearMonth.from(from).plusMonths(months);
        return target.atDay(Math.min(anchor.getDayOfMonth(), target.lengthOfMonth()));
    }

    private void advanceSchedule(Subscription subscription, LocalDate processedDueDate) {
        subscription.setNextBillingDate(nextDate(subscription, processedDueDate));
    }

    private SubscriptionOccurrence ownedOccurrence(Long userId, Long subscriptionId, Long occurrenceId) {
        return occurrenceRepository.findOwned(occurrenceId, subscriptionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription occurrence not found"));
    }

    private void validateCurrentOccurrence(Subscription subscription, SubscriptionOccurrence occurrence) {
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only active subscriptions can be processed");
        }
        if (occurrence.getStatus() != SubscriptionOccurrenceStatus.PLANNED
                || !occurrence.getDueDate().equals(subscription.getNextBillingDate())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This billing cycle is already processed");
        }
    }

    private Category findExpenseCategory(Long categoryId, Long userId) {
        Category category = expenseCategoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense category not found"));
        if (category.getType() != CategoryType.EXPENSE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subscription payments require an expense category");
        }
        return category;
    }

    private SubscriptionOccurrenceResponse occurrenceResponse(SubscriptionOccurrence occurrence) {
        return new SubscriptionOccurrenceResponse(occurrence.getId(), occurrence.getSubscription().getId(),
                occurrence.getSubscription().getName(), occurrence.getDueDate(), occurrence.getExpectedAmount(),
                occurrence.getActualAmount(), occurrence.getStatus(), occurrence.getPaidOn(),
                occurrence.getTransaction() == null ? null : occurrence.getTransaction().getId(), currency);
    }

    private BigDecimal zeroIfNull(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : amount;
    }

    private SubscriptionCategoryResponse categoryResponse(SubscriptionCategory category) {
        return new SubscriptionCategoryResponse(category.getId(), category.getName());
    }

    private BigDecimal monthlyEquivalent(BigDecimal amount, SubscriptionBillingInterval interval) {
        BigDecimal monthly = switch (interval) {
            case WEEKLY -> amount.multiply(FIFTY_TWO).divide(TWELVE, 2, RoundingMode.HALF_UP);
            case MONTHLY -> amount;
            case QUARTERLY -> amount.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
            case YEARLY -> amount.divide(TWELVE, 2, RoundingMode.HALF_UP);
        };
        return monthly.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeNote(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be non-negative and size must be between 1 and 100");
        }
    }
}
