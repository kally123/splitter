package com.splitter.expense.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Expense share data transfer object.
 */
@Builder
public record ExpenseShareDto(
    UUID id,
    UUID userId,
    String userDisplayName,
    BigDecimal shareAmount,
    BigDecimal sharePercentage,
    Integer shareUnits,
    boolean paid
) {}
