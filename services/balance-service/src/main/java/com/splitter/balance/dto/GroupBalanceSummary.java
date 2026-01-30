package com.splitter.balance.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO for group balance summary.
 */
@Builder
public record GroupBalanceSummary(
    UUID groupId,
    String groupName,
    String currency,
    BigDecimal totalExpenses,
    List<UserBalance> userBalances,
    List<SimplifiedDebt> simplifiedDebts
) {
    /**
     * Individual user's balance summary.
     */
    @Builder
    public record UserBalance(
        UUID userId,
        String displayName,
        BigDecimal paid,
        BigDecimal owes,
        BigDecimal netBalance
    ) {}

    /**
     * Simplified debt between two users.
     */
    @Builder
    public record SimplifiedDebt(
        UUID fromUserId,
        String fromUserDisplayName,
        UUID toUserId,
        String toUserDisplayName,
        BigDecimal amount,
        String currency
    ) {}
}
