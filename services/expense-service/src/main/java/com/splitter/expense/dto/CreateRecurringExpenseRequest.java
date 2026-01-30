package com.splitter.expense.dto;

import com.splitter.expense.model.ExpenseCategory;
import com.splitter.expense.model.RecurrenceFrequency;
import com.splitter.expense.model.SplitType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRecurringExpenseRequest {

    @NotNull(message = "Group ID is required")
    private UUID groupId;

    @NotBlank(message = "Description is required")
    @Size(max = 200, message = "Description cannot exceed 200 characters")
    private String description;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @Size(max = 3, message = "Currency code must be 3 characters")
    private String currency;

    private ExpenseCategory category;

    private SplitType splitType;

    private List<ExpenseShareRequest> splits;

    @NotNull(message = "Frequency is required")
    private RecurrenceFrequency frequency;

    @Min(value = 1, message = "Interval must be at least 1")
    private Integer intervalValue;

    @Min(value = 1, message = "Day of week must be between 1-7")
    @Max(value = 7, message = "Day of week must be between 1-7")
    private Integer dayOfWeek;

    @Min(value = 1, message = "Day of month must be between 1-31")
    @Max(value = 31, message = "Day of month must be between 1-31")
    private Integer dayOfMonth;

    @Min(value = 1, message = "Month must be between 1-12")
    @Max(value = 12, message = "Month must be between 1-12")
    private Integer monthOfYear;

    private LocalDate startDate;

    @Future(message = "End date must be in the future")
    private LocalDate endDate;
}
