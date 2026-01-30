package com.splitter.expense.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitter.expense.dto.CreateRecurringExpenseRequest;
import com.splitter.expense.dto.ExpenseShareRequest;
import com.splitter.expense.dto.RecurringExpenseResponse;
import com.splitter.expense.dto.UpdateRecurringExpenseRequest;
import com.splitter.expense.model.*;
import com.splitter.expense.repository.RecurringExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecurringExpenseService {

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final ExpenseService expenseService;
    private final ObjectMapper objectMapper;

    public Mono<RecurringExpenseResponse> create(CreateRecurringExpenseRequest request, UUID userId) {
        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();
        LocalDate nextOccurrence = calculateInitialNextOccurrence(request, startDate);

        RecurringExpense recurring = RecurringExpense.builder()
            .groupId(request.getGroupId())
            .createdBy(userId)
            .description(request.getDescription())
            .amount(request.getAmount())
            .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
            .category(request.getCategory())
            .splitType(request.getSplitType() != null ? request.getSplitType() : SplitType.EQUAL)
            .splitsJson(serializeSplits(request.getSplits()))
            .frequency(request.getFrequency())
            .intervalValue(request.getIntervalValue() != null ? request.getIntervalValue() : 1)
            .dayOfWeek(request.getDayOfWeek())
            .dayOfMonth(request.getDayOfMonth())
            .monthOfYear(request.getMonthOfYear())
            .startDate(startDate)
            .endDate(request.getEndDate())
            .nextOccurrence(nextOccurrence)
            .active(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        return recurringExpenseRepository.save(recurring)
            .map(this::toResponse)
            .doOnSuccess(r -> log.info("Created recurring expense: {} for group {}", r.getId(), r.getGroupId()));
    }

    public Flux<RecurringExpenseResponse> getByGroup(UUID groupId, UUID userId) {
        return recurringExpenseRepository.findByGroupId(groupId)
            .map(this::toResponse);
    }

    public Flux<RecurringExpenseResponse> getByUser(UUID userId) {
        return recurringExpenseRepository.findByCreatedBy(userId)
            .map(this::toResponse);
    }

    public Mono<RecurringExpenseResponse> getById(UUID id, UUID userId) {
        return recurringExpenseRepository.findActiveById(id)
            .map(this::toResponse);
    }

    public Mono<RecurringExpenseResponse> update(UUID id, UpdateRecurringExpenseRequest request, UUID userId) {
        return recurringExpenseRepository.findActiveById(id)
            .flatMap(existing -> {
                if (request.getDescription() != null) existing.setDescription(request.getDescription());
                if (request.getAmount() != null) existing.setAmount(request.getAmount());
                if (request.getCurrency() != null) existing.setCurrency(request.getCurrency());
                if (request.getCategory() != null) existing.setCategory(request.getCategory());
                if (request.getSplitType() != null) existing.setSplitType(request.getSplitType());
                if (request.getSplits() != null) existing.setSplitsJson(serializeSplits(request.getSplits()));
                if (request.getFrequency() != null) existing.setFrequency(request.getFrequency());
                if (request.getIntervalValue() != null) existing.setIntervalValue(request.getIntervalValue());
                if (request.getDayOfWeek() != null) existing.setDayOfWeek(request.getDayOfWeek());
                if (request.getDayOfMonth() != null) existing.setDayOfMonth(request.getDayOfMonth());
                if (request.getEndDate() != null) existing.setEndDate(request.getEndDate());
                
                existing.setUpdatedAt(Instant.now());
                
                // Recalculate next occurrence if schedule changed
                if (request.getFrequency() != null || request.getIntervalValue() != null) {
                    LocalDate from = existing.getLastGenerated() != null ? 
                        existing.getLastGenerated() : existing.getStartDate();
                    existing.setNextOccurrence(existing.calculateNextOccurrence(from));
                }
                
                return recurringExpenseRepository.save(existing);
            })
            .map(this::toResponse);
    }

    public Mono<RecurringExpenseResponse> pause(UUID id, UUID userId) {
        return recurringExpenseRepository.findActiveById(id)
            .flatMap(recurring -> {
                recurring.setActive(false);
                recurring.setUpdatedAt(Instant.now());
                return recurringExpenseRepository.save(recurring);
            })
            .map(this::toResponse)
            .doOnSuccess(r -> log.info("Paused recurring expense: {}", id));
    }

    public Mono<RecurringExpenseResponse> resume(UUID id, UUID userId) {
        return recurringExpenseRepository.findActiveById(id)
            .flatMap(recurring -> {
                recurring.setActive(true);
                // Recalculate next occurrence from today if it's in the past
                if (recurring.getNextOccurrence().isBefore(LocalDate.now())) {
                    recurring.setNextOccurrence(
                        recurring.calculateNextOccurrence(LocalDate.now()));
                }
                recurring.setUpdatedAt(Instant.now());
                return recurringExpenseRepository.save(recurring);
            })
            .map(this::toResponse)
            .doOnSuccess(r -> log.info("Resumed recurring expense: {}", id));
    }

    @Transactional
    public Mono<Void> delete(UUID id, UUID userId) {
        return recurringExpenseRepository.findActiveById(id)
            .flatMap(recurring -> {
                recurring.setDeleted(true);
                recurring.setActive(false);
                recurring.setUpdatedAt(Instant.now());
                return recurringExpenseRepository.save(recurring);
            })
            .doOnSuccess(r -> log.info("Deleted recurring expense: {}", id))
            .then();
    }

    private LocalDate calculateInitialNextOccurrence(CreateRecurringExpenseRequest request, LocalDate startDate) {
        // If start date is today or in the future, use it as first occurrence
        if (!startDate.isBefore(LocalDate.now())) {
            return startDate;
        }
        // Otherwise, calculate next occurrence from start date
        RecurringExpense temp = RecurringExpense.builder()
            .frequency(request.getFrequency())
            .intervalValue(request.getIntervalValue() != null ? request.getIntervalValue() : 1)
            .dayOfWeek(request.getDayOfWeek())
            .dayOfMonth(request.getDayOfMonth())
            .monthOfYear(request.getMonthOfYear())
            .build();
        
        LocalDate next = startDate;
        while (next.isBefore(LocalDate.now())) {
            next = temp.calculateNextOccurrence(next);
        }
        return next;
    }

    private String serializeSplits(List<ExpenseShareRequest> splits) {
        if (splits == null || splits.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(splits);
        } catch (Exception e) {
            log.error("Failed to serialize splits", e);
            return null;
        }
    }

    private List<ExpenseShareRequest> deserializeSplits(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to deserialize splits", e);
            return List.of();
        }
    }

    private RecurringExpenseResponse toResponse(RecurringExpense recurring) {
        return RecurringExpenseResponse.builder()
            .id(recurring.getId())
            .groupId(recurring.getGroupId())
            .createdBy(recurring.getCreatedBy())
            .description(recurring.getDescription())
            .amount(recurring.getAmount())
            .currency(recurring.getCurrency())
            .category(recurring.getCategory())
            .splitType(recurring.getSplitType())
            .splits(deserializeSplits(recurring.getSplitsJson()))
            .frequency(recurring.getFrequency())
            .intervalValue(recurring.getIntervalValue())
            .dayOfWeek(recurring.getDayOfWeek())
            .dayOfMonth(recurring.getDayOfMonth())
            .monthOfYear(recurring.getMonthOfYear())
            .startDate(recurring.getStartDate())
            .endDate(recurring.getEndDate())
            .nextOccurrence(recurring.getNextOccurrence())
            .lastGenerated(recurring.getLastGenerated())
            .isActive(recurring.isActive())
            .createdAt(recurring.getCreatedAt())
            .updatedAt(recurring.getUpdatedAt())
            .build();
    }
}
