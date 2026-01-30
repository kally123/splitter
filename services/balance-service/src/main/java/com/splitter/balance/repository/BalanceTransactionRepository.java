package com.splitter.balance.repository;

import com.splitter.balance.model.BalanceTransaction;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.UUID;

/**
 * Reactive repository for BalanceTransaction entities.
 */
@Repository
public interface BalanceTransactionRepository extends R2dbcRepository<BalanceTransaction, UUID> {

    /**
     * Find transactions for a group.
     */
    Flux<BalanceTransaction> findByGroupIdOrderByCreatedAtDesc(UUID groupId);

    /**
     * Find transactions involving a user.
     */
    @Query("SELECT * FROM balance_transactions " +
           "WHERE from_user_id = :userId OR to_user_id = :userId " +
           "ORDER BY created_at DESC")
    Flux<BalanceTransaction> findByUser(UUID userId);

    /**
     * Find transactions for a group within a date range.
     */
    @Query("SELECT * FROM balance_transactions " +
           "WHERE group_id = :groupId AND created_at BETWEEN :startDate AND :endDate " +
           "ORDER BY created_at DESC")
    Flux<BalanceTransaction> findByGroupIdAndDateRange(UUID groupId, Instant startDate, Instant endDate);

    /**
     * Find transactions by reference ID.
     */
    Flux<BalanceTransaction> findByReferenceId(UUID referenceId);

    /**
     * Find recent transactions for a user in a group.
     */
    @Query("SELECT * FROM balance_transactions " +
           "WHERE group_id = :groupId AND (from_user_id = :userId OR to_user_id = :userId) " +
           "ORDER BY created_at DESC LIMIT :limit")
    Flux<BalanceTransaction> findRecentByGroupAndUser(UUID groupId, UUID userId, int limit);
}
