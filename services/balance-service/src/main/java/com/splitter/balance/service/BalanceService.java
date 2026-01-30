package com.splitter.balance.service;

import com.splitter.balance.dto.BalanceDto;
import com.splitter.balance.dto.GroupBalanceSummary;
import com.splitter.balance.dto.UserBalanceSummary;
import com.splitter.balance.model.Balance;
import com.splitter.balance.model.BalanceTransaction;
import com.splitter.balance.repository.BalanceRepository;
import com.splitter.balance.repository.BalanceTransactionRepository;
import com.splitter.common.events.EventTopics;
import com.splitter.common.events.expense.ExpenseCreatedEvent;
import com.splitter.common.events.expense.ExpenseDeletedEvent;
import com.splitter.common.events.settlement.SettlementCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for balance calculations and debt management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceService {

    private final BalanceRepository balanceRepository;
    private final BalanceTransactionRepository transactionRepository;
    private final DebtSimplifier debtSimplifier;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String BALANCE_CACHE_PREFIX = "balance:group:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    /**
     * Get all balances for a group.
     */
    public Flux<BalanceDto> getGroupBalances(UUID groupId) {
        return balanceRepository.findByGroupId(groupId)
                .map(this::toDto);
    }

    /**
     * Get non-zero balances (active debts) for a group.
     */
    public Flux<BalanceDto> getActiveDebts(UUID groupId) {
        return balanceRepository.findNonZeroBalancesByGroupId(groupId)
                .map(this::toDto);
    }

    /**
     * Get balance summary for a group.
     */
    public Mono<GroupBalanceSummary> getGroupBalanceSummary(UUID groupId) {
        return balanceRepository.findNonZeroBalancesByGroupId(groupId)
                .collectList()
                .map(balances -> {
                    // Convert to simplified debts
                    List<DebtSimplifier.Debt> debts = balances.stream()
                            .filter(b -> b.getAmount().compareTo(BigDecimal.ZERO) > 0)
                            .map(b -> new DebtSimplifier.Debt(
                                    b.getFromUserId(),
                                    b.getToUserId(),
                                    b.getAmount()))
                            .collect(Collectors.toList());

                    List<DebtSimplifier.Debt> simplified = debtSimplifier.simplify(debts);

                    List<GroupBalanceSummary.SimplifiedDebt> simplifiedDebts = simplified.stream()
                            .map(d -> GroupBalanceSummary.SimplifiedDebt.builder()
                                    .fromUserId(d.fromUserId())
                                    .toUserId(d.toUserId())
                                    .amount(d.amount())
                                    .currency("USD")
                                    .build())
                            .collect(Collectors.toList());

                    return GroupBalanceSummary.builder()
                            .groupId(groupId)
                            .simplifiedDebts(simplifiedDebts)
                            .build();
                });
    }

    /**
     * Get balance between two users in a group.
     */
    public Mono<BalanceDto> getBalanceBetween(UUID groupId, UUID userId1, UUID userId2) {
        return balanceRepository.findByGroupIdAndFromUserIdAndToUserId(groupId, userId1, userId2)
                .map(this::toDto)
                .switchIfEmpty(Mono.just(BalanceDto.builder()
                        .groupId(groupId)
                        .fromUserId(userId1)
                        .toUserId(userId2)
                        .amount(BigDecimal.ZERO)
                        .currency("USD")
                        .build()));
    }

    /**
     * Get all balances involving a user.
     */
    public Flux<BalanceDto> getUserBalances(UUID userId) {
        return Flux.merge(
                balanceRepository.findByFromUserId(userId),
                balanceRepository.findByToUserId(userId)
        ).map(this::toDto);
    }

    /**
     * Process expense creation event to update balances.
     */
    @Transactional
    public Mono<Void> processExpenseCreated(ExpenseCreatedEvent event) {
        log.info("Processing expense created event: {}", event.getExpenseId());

        UUID groupId = event.getGroupId();
        UUID paidBy = event.getPaidBy();

        // Create balance updates for each share
        return Flux.fromIterable(event.getShares())
                .filter(share -> !share.getUserId().equals(paidBy)) // Exclude payer
                .flatMap(share -> updateBalance(
                        groupId,
                        share.getUserId(), // owes money
                        paidBy,            // is owed money
                        share.getAmount(),
                        event.getCurrency(),
                        BalanceTransaction.TransactionType.EXPENSE,
                        event.getExpenseId(),
                        event.getDescription()
                ))
                .then()
                .doOnSuccess(v -> invalidateCache(groupId));
    }

    /**
     * Process settlement event to update balances.
     */
    @Transactional
    public Mono<Void> processSettlement(SettlementCreatedEvent event) {
        log.info("Processing settlement event: {}", event.getSettlementId());

        return updateBalance(
                event.getGroupId(),
                event.getToUserId(),   // Was owed, now receiving less
                event.getFromUserId(), // Was owing, now owes less
                event.getAmount().negate(), // Reduce the debt
                event.getCurrency(),
                BalanceTransaction.TransactionType.SETTLEMENT,
                event.getSettlementId(),
                "Settlement payment"
        ).then()
        .doOnSuccess(v -> invalidateCache(event.getGroupId()));
    }

    /**
     * Update balance between two users.
     */
    private Mono<Balance> updateBalance(
            UUID groupId,
            UUID fromUserId,
            UUID toUserId,
            BigDecimal amount,
            String currency,
            BalanceTransaction.TransactionType type,
            UUID referenceId,
            String description) {

        // Record the transaction
        BalanceTransaction transaction = BalanceTransaction.builder()
                .groupId(groupId)
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .amount(amount)
                .currency(currency)
                .transactionType(type)
                .referenceId(referenceId)
                .description(description)
                .createdAt(Instant.now())
                .build();

        return transactionRepository.save(transaction)
                .then(balanceRepository.findByGroupIdAndFromUserIdAndToUserId(groupId, fromUserId, toUserId)
                        .flatMap(balance -> {
                            balance.setAmount(balance.getAmount().add(amount));
                            balance.setUpdatedAt(Instant.now());
                            return balanceRepository.save(balance);
                        })
                        .switchIfEmpty(Mono.defer(() -> {
                            // Check reverse direction
                            return balanceRepository.findByGroupIdAndFromUserIdAndToUserId(groupId, toUserId, fromUserId)
                                    .flatMap(reverseBalance -> {
                                        reverseBalance.setAmount(reverseBalance.getAmount().subtract(amount));
                                        reverseBalance.setUpdatedAt(Instant.now());
                                        return balanceRepository.save(reverseBalance);
                                    })
                                    .switchIfEmpty(Mono.defer(() -> {
                                        // Create new balance
                                        Balance newBalance = Balance.builder()
                                                .groupId(groupId)
                                                .fromUserId(fromUserId)
                                                .toUserId(toUserId)
                                                .amount(amount)
                                                .currency(currency)
                                                .updatedAt(Instant.now())
                                                .build();
                                        return balanceRepository.save(newBalance);
                                    }));
                        })));
    }

    /**
     * Invalidate cached balance data.
     */
    private void invalidateCache(UUID groupId) {
        redisTemplate.delete(BALANCE_CACHE_PREFIX + groupId).subscribe();
    }

    /**
     * Kafka listener for expense events.
     */
    @KafkaListener(topics = EventTopics.EXPENSE_EVENTS, groupId = "balance-service")
    public void handleExpenseEvent(Object event) {
        if (event instanceof ExpenseCreatedEvent expenseCreated) {
            processExpenseCreated(expenseCreated).subscribe();
        } else if (event instanceof ExpenseDeletedEvent expenseDeleted) {
            log.info("Expense deleted: {} - balance recalculation may be needed", 
                    expenseDeleted.getExpenseId());
            // TODO: Handle expense deletion - recalculate balances
        }
    }

    /**
     * Kafka listener for settlement events.
     */
    @KafkaListener(topics = EventTopics.SETTLEMENT_EVENTS, groupId = "balance-service")
    public void handleSettlementEvent(Object event) {
        if (event instanceof SettlementCreatedEvent settlementCreated) {
            processSettlement(settlementCreated).subscribe();
        }
    }

    private BalanceDto toDto(Balance balance) {
        return BalanceDto.builder()
                .id(balance.getId())
                .groupId(balance.getGroupId())
                .fromUserId(balance.getFromUserId())
                .toUserId(balance.getToUserId())
                .amount(balance.getAmount())
                .currency(balance.getCurrency())
                .updatedAt(balance.getUpdatedAt())
                .build();
    }
}
