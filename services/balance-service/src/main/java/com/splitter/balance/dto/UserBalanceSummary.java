package com.splitter.balance.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO for user balance summary across all groups.
 */
@Builder
public record UserBalanceSummary(
    UUID userId,
    BigDecimal totalOwed,
    BigDecimal totalOwing,
    BigDecimal netBalance,
    String primaryCurrency,
    List<GroupDebt> groupDebts
) {
    /**
     * Debt summary for a specific group.
     */
    @Builder
    public record GroupDebt(
        UUID groupId,
        String groupName,
        BigDecimal netBalance,
        String currency,
        List<DebtDetail> debts
    ) {}

    /**
     * Individual debt detail.
     */
    @Builder
    public record DebtDetail(
        UUID userId,
        String displayName,
        BigDecimal amount,
        boolean isOwed
    ) {}
}
