package com.splitter.expense.dto;

import com.splitter.expense.model.Expense;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request for creating a new expense.
 */
public record CreateExpenseRequest(
    @NotNull(message = "Group ID is required")
    UUID groupId,

    @NotBlank(message = "Description is required")
    @Size(max = 200, message = "Description must be at most 200 characters")
    String description,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,

    @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
    String currency,

    @NotNull(message = "Paid by user ID is required")
    UUID paidBy,

    Expense.SplitType splitType,

    Expense.ExpenseCategory category,

    LocalDate date,

    @Size(max = 500, message = "Notes must be at most 500 characters")
    String notes,

    String receiptUrl,

    @NotEmpty(message = "At least one participant is required")
    List<UUID> participants,

    @Valid
    List<ShareDetail> shares
) {
    /**
     * Detail for non-equal splits.
     */
    public record ShareDetail(
        @NotNull UUID userId,
        BigDecimal amount,
        BigDecimal percentage,
        Integer units
    ) {}
}
