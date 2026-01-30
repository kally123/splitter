package com.splitter.common.dto.expense;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request object for creating a new expense.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExpenseRequest {

    private UUID groupId;

    @NotBlank(message = "Description is required")
    @Size(max = 255, message = "Description must be less than 255 characters")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 13, fraction = 2, message = "Amount format is invalid")
    private BigDecimal amount;

    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    private String currency;

    private UUID categoryId;

    @NotNull(message = "Paid by user ID is required")
    private UUID paidBy;

    private ExpenseDto.SplitType splitType;

    private LocalDate expenseDate;

    @Size(max = 500, message = "Notes must be less than 500 characters")
    private String notes;

    @NotEmpty(message = "At least one participant is required")
    @Valid
    private List<ParticipantShare> participants;

    /**
     * Participant share in the expense.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantShare {

        @NotNull(message = "User ID is required")
        private UUID userId;

        /**
         * For EXACT split: the exact amount
         * For PERCENTAGE split: the percentage (0-100)
         * For SHARES split: number of shares
         * For EQUAL split: ignored
         */
        private BigDecimal value;
    }
}
