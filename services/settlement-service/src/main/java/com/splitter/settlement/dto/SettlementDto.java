package com.splitter.settlement.dto;

import com.splitter.settlement.model.Settlement;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Settlement data transfer object.
 */
@Builder
public record SettlementDto(
    UUID id,
    UUID groupId,
    UUID fromUserId,
    String fromUserDisplayName,
    UUID toUserId,
    String toUserDisplayName,
    BigDecimal amount,
    String currency,
    Settlement.PaymentMethod paymentMethod,
    Settlement.SettlementStatus status,
    LocalDate settlementDate,
    String notes,
    String externalReference,
    UUID createdBy,
    UUID confirmedBy,
    Instant confirmedAt,
    Instant createdAt,
    Instant updatedAt
) {}
