package com.splitter.common.dto.balance;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents a simplified debt between two users.
 * Used by the debt simplification algorithm.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SimplifiedDebtDto {

    /**
     * User who owes money.
     */
    private UUID fromUserId;
    private String fromUserName;
    private String fromUserAvatarUrl;

    /**
     * User who is owed money.
     */
    private UUID toUserId;
    private String toUserName;
    private String toUserAvatarUrl;

    /**
     * Amount owed.
     */
    private BigDecimal amount;

    /**
     * Currency.
     */
    private String currency;

    /**
     * Group context (null for overall).
     */
    private UUID groupId;
    private String groupName;
}
