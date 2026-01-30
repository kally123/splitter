package com.splitter.common.dto.expense;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.splitter.common.dto.MoneyDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Expense data transfer object.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExpenseDto {

    private UUID id;
    private UUID groupId;
    private String description;
    private BigDecimal amount;
    private String currency;
    private UUID categoryId;
    private String categoryName;
    private UUID paidBy;
    private String paidByName;
    private SplitType splitType;
    private LocalDate expenseDate;
    private String receiptUrl;
    private String notes;
    private List<ExpenseShareDto> shares;
    private UUID createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Expense split types.
     */
    public enum SplitType {
        EQUAL,       // Split equally among participants
        PERCENTAGE,  // Split by percentage
        SHARES,      // Split by number of shares
        EXACT        // Exact amounts specified
    }
}
