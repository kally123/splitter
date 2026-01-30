package com.splitter.common.dto.expense;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Expense share data transfer object.
 * Represents how much each participant owes for an expense.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExpenseShareDto {

    private UUID id;
    private UUID expenseId;
    private UUID userId;
    private String userName;
    private BigDecimal shareAmount;
    private BigDecimal sharePercentage;
    private Integer shareUnits;
    private boolean isPayer;
}
