package com.splitter.expense.repository;

import com.splitter.expense.model.Expense;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Reactive repository for Expense entities.
 */
@Repository
public interface ExpenseRepository extends R2dbcRepository<Expense, UUID> {

    /**
     * Find expenses by group ID with pagination.
     */
    @Query("SELECT * FROM expenses WHERE group_id = :groupId AND is_deleted = false ORDER BY expense_date DESC, created_at DESC LIMIT :limit OFFSET :offset")
    Flux<Expense> findByGroupId(UUID groupId, int limit, int offset);

    /**
     * Find expenses by group ID.
     */
    Flux<Expense> findByGroupIdAndDeletedFalseOrderByExpenseDateDesc(UUID groupId);

    /**
     * Find expenses paid by a user.
     */
    Flux<Expense> findByPaidByAndDeletedFalse(UUID paidBy);

    /**
     * Find expenses in a group by date range.
     */
    @Query("SELECT * FROM expenses WHERE group_id = :groupId AND is_deleted = false " +
           "AND expense_date BETWEEN :startDate AND :endDate ORDER BY expense_date DESC")
    Flux<Expense> findByGroupIdAndDateRange(UUID groupId, LocalDate startDate, LocalDate endDate);

    /**
     * Find expenses by category in a group.
     */
    Flux<Expense> findByGroupIdAndCategoryAndDeletedFalse(UUID groupId, Expense.ExpenseCategory category);

    /**
     * Count expenses in a group.
     */
    Mono<Long> countByGroupIdAndDeletedFalse(UUID groupId);

    /**
     * Sum of expenses in a group.
     */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE group_id = :groupId AND is_deleted = false")
    Mono<java.math.BigDecimal> sumByGroupId(UUID groupId);

    /**
     * Find recent expenses for a user across all groups.
     */
    @Query("SELECT e.* FROM expenses e " +
           "JOIN expense_shares es ON e.id = es.expense_id " +
           "WHERE es.user_id = :userId AND e.is_deleted = false " +
           "ORDER BY e.expense_date DESC, e.created_at DESC LIMIT :limit")
    Flux<Expense> findRecentExpensesForUser(UUID userId, int limit);
}
