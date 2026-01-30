package com.splitter.expense.repository;

import com.splitter.expense.model.ExpenseShare;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for ExpenseShare entities.
 */
@Repository
public interface ExpenseShareRepository extends R2dbcRepository<ExpenseShare, UUID> {

    /**
     * Find all shares for an expense.
     */
    Flux<ExpenseShare> findByExpenseId(UUID expenseId);

    /**
     * Find all shares for a user.
     */
    Flux<ExpenseShare> findByUserId(UUID userId);

    /**
     * Find share for a specific user in an expense.
     */
    Mono<ExpenseShare> findByExpenseIdAndUserId(UUID expenseId, UUID userId);

    /**
     * Delete all shares for an expense.
     */
    Mono<Void> deleteByExpenseId(UUID expenseId);

    /**
     * Find unpaid shares for a user.
     */
    Flux<ExpenseShare> findByUserIdAndPaidFalse(UUID userId);

    /**
     * Mark shares as paid.
     */
    @Modifying
    @Query("UPDATE expense_shares SET is_paid = true WHERE expense_id = :expenseId AND user_id = :userId")
    Mono<Integer> markAsPaid(UUID expenseId, UUID userId);

    /**
     * Get user IDs who have shares in an expense.
     */
    @Query("SELECT DISTINCT user_id FROM expense_shares WHERE expense_id = :expenseId")
    Flux<UUID> findUserIdsByExpenseId(UUID expenseId);
}
