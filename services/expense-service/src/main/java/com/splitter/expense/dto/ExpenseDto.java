package com.splitter.expense.dto;

import com.splitter.expense.model.Expense;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Expense data transfer object.
 */
@Builder
public record ExpenseDto(
    UUID id,
    UUID groupId,
    String description,
    BigDecimal amount,
    String currency,
    UUID paidBy,
    String paidByDisplayName,
    Expense.SplitType splitType,
    Expense.ExpenseCategory category,
    LocalDate expenseDate,
    String notes,
    String receiptUrl,
    UUID createdBy,
    List<ExpenseShareDto> shares,
    Instant createdAt,
    Instant updatedAt
) {}
