package com.splitter.expense.dto;

import com.splitter.expense.model.Expense;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request for updating an expense.
 */
public record UpdateExpenseRequest(
    @Size(max = 200, message = "Description must be at most 200 characters")
    String description,

    @Positive(message = "Amount must be positive")
    BigDecimal amount,

    @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
    String currency,

    UUID paidBy,

    Expense.SplitType splitType,

    Expense.ExpenseCategory category,

    LocalDate date,

    @Size(max = 500, message = "Notes must be at most 500 characters")
    String notes,

    String receiptUrl,

    List<UUID> participants,

    @Valid
    List<CreateExpenseRequest.ShareDetail> shares
) {}
