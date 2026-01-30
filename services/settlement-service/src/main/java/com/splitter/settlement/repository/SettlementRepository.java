package com.splitter.settlement.repository;

import com.splitter.settlement.model.Settlement;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Reactive repository for Settlement entities.
 */
@Repository
public interface SettlementRepository extends R2dbcRepository<Settlement, UUID> {

    /**
     * Find settlements by group.
     */
    Flux<Settlement> findByGroupIdOrderByCreatedAtDesc(UUID groupId);

    /**
     * Find settlements where user paid.
     */
    Flux<Settlement> findByFromUserIdOrderByCreatedAtDesc(UUID fromUserId);

    /**
     * Find settlements where user received.
     */
    Flux<Settlement> findByToUserIdOrderByCreatedAtDesc(UUID toUserId);

    /**
     * Find settlements involving a user (either direction).
     */
    @Query("SELECT * FROM settlements WHERE from_user_id = :userId OR to_user_id = :userId ORDER BY created_at DESC")
    Flux<Settlement> findByUser(UUID userId);

    /**
     * Find settlements between two users in a group.
     */
    @Query("SELECT * FROM settlements WHERE group_id = :groupId " +
           "AND ((from_user_id = :userId1 AND to_user_id = :userId2) " +
           "OR (from_user_id = :userId2 AND to_user_id = :userId1)) " +
           "ORDER BY created_at DESC")
    Flux<Settlement> findByGroupAndUsers(UUID groupId, UUID userId1, UUID userId2);

    /**
     * Find pending settlements for a user to confirm.
     */
    Flux<Settlement> findByToUserIdAndStatus(UUID toUserId, Settlement.SettlementStatus status);

    /**
     * Find settlements by date range.
     */
    @Query("SELECT * FROM settlements WHERE group_id = :groupId " +
           "AND settlement_date BETWEEN :startDate AND :endDate ORDER BY settlement_date DESC")
    Flux<Settlement> findByGroupAndDateRange(UUID groupId, LocalDate startDate, LocalDate endDate);

    /**
     * Count pending settlements for a user.
     */
    Mono<Long> countByToUserIdAndStatus(UUID toUserId, Settlement.SettlementStatus status);

    /**
     * Sum of confirmed settlements in a group.
     */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM settlements WHERE group_id = :groupId AND status = 'CONFIRMED'")
    Mono<java.math.BigDecimal> sumConfirmedByGroupId(UUID groupId);
}
