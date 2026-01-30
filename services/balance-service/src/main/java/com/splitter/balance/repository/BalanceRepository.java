package com.splitter.balance.repository;

import com.splitter.balance.model.Balance;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Reactive repository for Balance entities.
 */
@Repository
public interface BalanceRepository extends R2dbcRepository<Balance, UUID> {

    /**
     * Find balance between two users in a group.
     */
    Mono<Balance> findByGroupIdAndFromUserIdAndToUserId(UUID groupId, UUID fromUserId, UUID toUserId);

    /**
     * Find all balances for a group.
     */
    Flux<Balance> findByGroupId(UUID groupId);

    /**
     * Find all balances where a user owes money.
     */
    Flux<Balance> findByFromUserId(UUID fromUserId);

    /**
     * Find all balances where a user is owed money.
     */
    Flux<Balance> findByToUserId(UUID toUserId);

    /**
     * Find balances involving a specific user in a group.
     */
    @Query("SELECT * FROM balances WHERE group_id = :groupId AND (from_user_id = :userId OR to_user_id = :userId)")
    Flux<Balance> findByGroupIdAndUser(UUID groupId, UUID userId);

    /**
     * Find non-zero balances for a group.
     */
    @Query("SELECT * FROM balances WHERE group_id = :groupId AND amount != 0")
    Flux<Balance> findNonZeroBalancesByGroupId(UUID groupId);

    /**
     * Update balance amount.
     */
    @Modifying
    @Query("UPDATE balances SET amount = amount + :delta, updated_at = NOW() " +
           "WHERE group_id = :groupId AND from_user_id = :fromUserId AND to_user_id = :toUserId")
    Mono<Integer> updateBalanceAmount(UUID groupId, UUID fromUserId, UUID toUserId, BigDecimal delta);

    /**
     * Delete all balances for a group.
     */
    Mono<Void> deleteByGroupId(UUID groupId);

    /**
     * Check if any non-zero balance exists for a group.
     */
    @Query("SELECT COUNT(*) > 0 FROM balances WHERE group_id = :groupId AND amount != 0")
    Mono<Boolean> hasNonZeroBalances(UUID groupId);
}
