package com.splitter.settlement.dto;

import com.splitter.settlement.model.Settlement;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request for creating a new settlement.
 */
public record CreateSettlementRequest(
    @NotNull(message = "Group ID is required")
    UUID groupId,

    @NotNull(message = "Recipient user ID is required")
    UUID toUserId,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,

    @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
    String currency,

    Settlement.PaymentMethod paymentMethod,

    LocalDate settlementDate,

    @Size(max = 500, message = "Notes must be at most 500 characters")
    String notes,

    String externalReference
) {}
