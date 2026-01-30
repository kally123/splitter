package com.splitter.common.dto.balance;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Balance summary for a user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BalanceSummaryDto {

    private UUID userId;
    private String userName;

    /**
     * Total amount the user owes to others.
     */
    private BigDecimal totalOwed;

    /**
     * Total amount others owe to the user.
     */
    private BigDecimal totalOwing;

    /**
     * Net balance (positive = others owe you, negative = you owe others).
     */
    private BigDecimal netBalance;

    /**
     * Default currency for this summary.
     */
    private String currency;

    /**
     * Detailed balances with each user.
     */
    private List<UserBalanceDto> balances;

    /**
     * Balance with a specific user.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserBalanceDto {
        private UUID userId;
        private String userName;
        private String avatarUrl;
        private BigDecimal amount; // positive = they owe you, negative = you owe them
        private String currency;
        private UUID groupId; // null for overall balance
        private String groupName;
    }
}
