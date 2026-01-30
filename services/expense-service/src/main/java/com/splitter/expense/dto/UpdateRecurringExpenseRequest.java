package com.splitter.expense.dto;

import com.splitter.expense.model.ExpenseCategory;
import com.splitter.expense.model.RecurrenceFrequency;
import com.splitter.expense.model.SplitType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRecurringExpenseRequest {

    @Size(max = 200, message = "Description cannot exceed 200 characters")
    private String description;

    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @Size(max = 3, message = "Currency code must be 3 characters")
    private String currency;

    private ExpenseCategory category;

    private SplitType splitType;

    private List<ExpenseShareRequest> splits;

    private RecurrenceFrequency frequency;

    @Min(value = 1, message = "Interval must be at least 1")
    private Integer intervalValue;

    @Min(value = 1, message = "Day of week must be between 1-7")
    @Max(value = 7, message = "Day of week must be between 1-7")
    private Integer dayOfWeek;

    @Min(value = 1, message = "Day of month must be between 1-31")
    @Max(value = 31, message = "Day of month must be between 1-31")
    private Integer dayOfMonth;

    private LocalDate endDate;
}
