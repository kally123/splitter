package com.splitter.expense.scheduler;

import com.splitter.expense.dto.CreateExpenseRequest;
import com.splitter.expense.dto.ExpenseShareRequest;
import com.splitter.expense.model.RecurringExpense;
import com.splitter.expense.repository.RecurringExpenseRepository;
import com.splitter.expense.service.ExpenseService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduler for processing recurring expenses and generating actual expenses.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RecurringExpenseScheduler {

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final ExpenseService expenseService;
    private final ObjectMapper objectMapper;

    /**
     * Process due recurring expenses daily at 1 AM.
     */
    @Scheduled(cron = "${recurring.scheduler.cron:0 0 1 * * *}")
    public void processRecurringExpenses() {
        log.info("Starting recurring expense processing");
        LocalDate today = LocalDate.now();

        recurringExpenseRepository.findDueRecurringExpenses(today)
            .flatMap(recurring -> processRecurringExpense(recurring, today))
            .doOnComplete(() -> log.info("Completed recurring expense processing"))
            .doOnError(error -> log.error("Error processing recurring expenses", error))
            .subscribe();
    }

    /**
     * Process a single recurring expense.
     */
    private Mono<Void> processRecurringExpense(RecurringExpense recurring, LocalDate date) {
        log.debug("Processing recurring expense: {} - {}", recurring.getId(), recurring.getDescription());

        // Create expense from template
        CreateExpenseRequest request = CreateExpenseRequest.builder()
            .groupId(recurring.getGroupId())
            .description(recurring.getDescription())
            .amount(recurring.getAmount())
            .currency(recurring.getCurrency())
            .category(recurring.getCategory())
            .splitType(recurring.getSplitType())
            .shares(deserializeSplits(recurring.getSplitsJson()))
            .expenseDate(date)
            .recurringExpenseId(recurring.getId())
            .build();

        return expenseService.createExpense(request, recurring.getCreatedBy())
            .flatMap(expense -> {
                log.info("Generated expense {} from recurring {}", expense.getId(), recurring.getId());
                
                // Calculate next occurrence
                LocalDate nextOccurrence = recurring.calculateNextOccurrence(date);
                
                // Check if we've passed the end date
                if (recurring.getEndDate() != null && nextOccurrence.isAfter(recurring.getEndDate())) {
                    log.info("Recurring expense {} has ended", recurring.getId());
                    recurring.setActive(false);
                }
                
                recurring.setNextOccurrence(nextOccurrence);
                recurring.setLastGenerated(date);
                
                return recurringExpenseRepository.save(recurring);
            })
            .doOnError(error -> log.error("Failed to process recurring expense: {}", recurring.getId(), error))
            .then();
    }

    /**
     * Manual trigger for processing recurring expenses (for testing).
     */
    public Mono<Long> processNow() {
        log.info("Manual trigger for recurring expense processing");
        LocalDate today = LocalDate.now();

        return recurringExpenseRepository.findDueRecurringExpenses(today)
            .flatMap(recurring -> processRecurringExpense(recurring, today).thenReturn(1L))
            .count()
            .doOnSuccess(count -> log.info("Processed {} recurring expenses", count));
    }

    private List<ExpenseShareRequest> deserializeSplits(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to deserialize splits", e);
            return null;
        }
    }
}
