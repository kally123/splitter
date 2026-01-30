package com.splitter.expense.service;

import com.splitter.common.events.EventTopics;
import com.splitter.common.events.expense.ExpenseCreatedEvent;
import com.splitter.common.events.expense.ExpenseDeletedEvent;
import com.splitter.expense.dto.*;
import com.splitter.expense.exception.ExpenseNotFoundException;
import com.splitter.expense.model.Expense;
import com.splitter.expense.model.ExpenseShare;
import com.splitter.expense.repository.ExpenseRepository;
import com.splitter.expense.repository.ExpenseShareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for expense management operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseShareRepository shareRepository;
    private final SplitCalculator splitCalculator;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Create a new expense.
     */
    @Transactional
    public Mono<ExpenseDto> createExpense(CreateExpenseRequest request, UUID creatorId) {
        log.info("Creating expense '{}' in group {} by user {}", 
                request.description(), request.groupId(), creatorId);

        Expense expense = Expense.builder()
                .groupId(request.groupId())
                .description(request.description())
                .amount(request.amount())
                .currency(request.currency() != null ? request.currency() : "USD")
                .paidBy(request.paidBy())
                .splitType(request.splitType() != null ? request.splitType() : Expense.SplitType.EQUAL)
                .category(request.category())
                .expenseDate(request.date() != null ? request.date() : LocalDate.now())
                .notes(request.notes())
                .receiptUrl(request.receiptUrl())
                .createdBy(creatorId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return expenseRepository.save(expense)
                .flatMap(savedExpense -> createShares(savedExpense, request)
                        .collectList()
                        .map(shares -> toDto(savedExpense, shares)))
                .doOnSuccess(this::publishExpenseCreatedEvent);
    }

    /**
     * Get expense by ID.
     */
    public Mono<ExpenseDto> getExpenseById(UUID expenseId) {
        return expenseRepository.findById(expenseId)
                .filter(expense -> !expense.isDeleted())
                .switchIfEmpty(Mono.error(new ExpenseNotFoundException(expenseId)))
                .flatMap(this::enrichWithShares);
    }

    /**
     * Get expenses for a group with pagination.
     */
    public Flux<ExpenseDto> getGroupExpenses(UUID groupId, int page, int size) {
        int offset = page * size;
        return expenseRepository.findByGroupId(groupId, size, offset)
                .flatMap(this::enrichWithShares);
    }

    /**
     * Get all expenses for a group.
     */
    public Flux<ExpenseDto> getAllGroupExpenses(UUID groupId) {
        return expenseRepository.findByGroupIdAndDeletedFalseOrderByExpenseDateDesc(groupId)
                .flatMap(this::enrichWithShares);
    }

    /**
     * Get expenses by date range.
     */
    public Flux<ExpenseDto> getExpensesByDateRange(UUID groupId, LocalDate startDate, LocalDate endDate) {
        return expenseRepository.findByGroupIdAndDateRange(groupId, startDate, endDate)
                .flatMap(this::enrichWithShares);
    }

    /**
     * Get recent expenses for a user.
     */
    public Flux<ExpenseDto> getRecentExpensesForUser(UUID userId, int limit) {
        return expenseRepository.findRecentExpensesForUser(userId, limit)
                .flatMap(this::enrichWithShares);
    }

    /**
     * Update an expense.
     */
    @Transactional
    public Mono<ExpenseDto> updateExpense(UUID expenseId, UpdateExpenseRequest request, UUID requesterId) {
        return expenseRepository.findById(expenseId)
                .filter(expense -> !expense.isDeleted())
                .switchIfEmpty(Mono.error(new ExpenseNotFoundException(expenseId)))
                .flatMap(expense -> {
                    if (request.description() != null) expense.setDescription(request.description());
                    if (request.amount() != null) expense.setAmount(request.amount());
                    if (request.currency() != null) expense.setCurrency(request.currency());
                    if (request.paidBy() != null) expense.setPaidBy(request.paidBy());
                    if (request.category() != null) expense.setCategory(request.category());
                    if (request.date() != null) expense.setExpenseDate(request.date());
                    if (request.notes() != null) expense.setNotes(request.notes());
                    expense.setUpdatedAt(Instant.now());

                    return expenseRepository.save(expense);
                })
                .flatMap(expense -> {
                    // If split details changed, recalculate shares
                    if (request.participants() != null || request.shares() != null) {
                        return shareRepository.deleteByExpenseId(expense.getId())
                                .then(Mono.just(expense));
                        // TODO: Recreate shares with new details
                    }
                    return Mono.just(expense);
                })
                .flatMap(this::enrichWithShares);
    }

    /**
     * Delete an expense (soft delete).
     */
    @Transactional
    public Mono<Void> deleteExpense(UUID expenseId, UUID requesterId) {
        return expenseRepository.findById(expenseId)
                .filter(expense -> !expense.isDeleted())
                .switchIfEmpty(Mono.error(new ExpenseNotFoundException(expenseId)))
                .flatMap(expense -> {
                    expense.setDeleted(true);
                    expense.setDeletedAt(Instant.now());
                    expense.setUpdatedAt(Instant.now());
                    return expenseRepository.save(expense);
                })
                .doOnSuccess(this::publishExpenseDeletedEvent)
                .then();
    }

    /**
     * Get expense count for a group.
     */
    public Mono<Long> getExpenseCount(UUID groupId) {
        return expenseRepository.countByGroupIdAndDeletedFalse(groupId);
    }

    /**
     * Get total expense amount for a group.
     */
    public Mono<BigDecimal> getTotalExpenseAmount(UUID groupId) {
        return expenseRepository.sumByGroupId(groupId);
    }

    // Private helper methods

    private Flux<ExpenseShare> createShares(Expense expense, CreateExpenseRequest request) {
        Map<UUID, BigDecimal> exactAmounts = new HashMap<>();
        Map<UUID, BigDecimal> percentages = new HashMap<>();
        Map<UUID, Integer> units = new HashMap<>();

        if (request.shares() != null) {
            for (CreateExpenseRequest.ShareDetail detail : request.shares()) {
                if (detail.amount() != null) exactAmounts.put(detail.userId(), detail.amount());
                if (detail.percentage() != null) percentages.put(detail.userId(), detail.percentage());
                if (detail.units() != null) units.put(detail.userId(), detail.units());
            }
        }

        List<ExpenseShare> shares = splitCalculator.calculateShares(
                expense.getId(),
                expense.getAmount(),
                expense.getSplitType(),
                request.participants(),
                exactAmounts,
                percentages,
                units
        );

        return Flux.fromIterable(shares)
                .flatMap(shareRepository::save);
    }

    private Mono<ExpenseDto> enrichWithShares(Expense expense) {
        return shareRepository.findByExpenseId(expense.getId())
                .collectList()
                .map(shares -> toDto(expense, shares));
    }

    private void publishExpenseCreatedEvent(ExpenseDto expense) {
        List<ExpenseCreatedEvent.ShareInfo> shareInfos = expense.shares().stream()
                .map(share -> ExpenseCreatedEvent.ShareInfo.builder()
                        .userId(share.userId())
                        .amount(share.shareAmount())
                        .build())
                .collect(Collectors.toList());

        ExpenseCreatedEvent event = ExpenseCreatedEvent.builder()
                .expenseId(expense.id())
                .groupId(expense.groupId())
                .description(expense.description())
                .amount(expense.amount())
                .currency(expense.currency())
                .paidBy(expense.paidBy())
                .shares(shareInfos)
                .build();

        kafkaTemplate.send(EventTopics.EXPENSE_EVENTS, expense.id().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish ExpenseCreatedEvent for expense: {}", expense.id(), ex);
                    } else {
                        log.info("Published ExpenseCreatedEvent for expense: {}", expense.id());
                    }
                });
    }

    private void publishExpenseDeletedEvent(Expense expense) {
        ExpenseDeletedEvent event = ExpenseDeletedEvent.builder()
                .expenseId(expense.getId())
                .groupId(expense.getGroupId())
                .build();

        kafkaTemplate.send(EventTopics.EXPENSE_EVENTS, expense.getId().toString(), event);
    }

    private ExpenseDto toDto(Expense expense, List<ExpenseShare> shares) {
        List<ExpenseShareDto> shareDtos = shares.stream()
                .map(share -> ExpenseShareDto.builder()
                        .id(share.getId())
                        .userId(share.getUserId())
                        .shareAmount(share.getShareAmount())
                        .sharePercentage(share.getSharePercentage())
                        .shareUnits(share.getShareUnits())
                        .paid(share.isPaid())
                        .build())
                .collect(Collectors.toList());

        return ExpenseDto.builder()
                .id(expense.getId())
                .groupId(expense.getGroupId())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .currency(expense.getCurrency())
                .paidBy(expense.getPaidBy())
                .splitType(expense.getSplitType())
                .category(expense.getCategory())
                .expenseDate(expense.getExpenseDate())
                .notes(expense.getNotes())
                .receiptUrl(expense.getReceiptUrl())
                .createdBy(expense.getCreatedBy())
                .shares(shareDtos)
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }
}
