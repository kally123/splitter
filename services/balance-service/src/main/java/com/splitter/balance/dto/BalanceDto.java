package com.splitter.balance.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Balance data transfer object.
 */
@Builder
public record BalanceDto(
    UUID id,
    UUID groupId,
    UUID fromUserId,
    String fromUserDisplayName,
    UUID toUserId,
    String toUserDisplayName,
    BigDecimal amount,
    String currency,
    Instant updatedAt
) {}
