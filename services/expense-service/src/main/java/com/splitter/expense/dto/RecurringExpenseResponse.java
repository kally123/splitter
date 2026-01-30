package com.splitter.expense.dto;

import com.splitter.expense.model.ExpenseCategory;
import com.splitter.expense.model.RecurrenceFrequency;
import com.splitter.expense.model.SplitType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringExpenseResponse {

    private UUID id;
    private UUID groupId;
    private UUID createdBy;
    private String description;
    private BigDecimal amount;
    private String currency;
    private ExpenseCategory category;
    private SplitType splitType;
    private List<ExpenseShareRequest> splits;
    private RecurrenceFrequency frequency;
    private Integer intervalValue;
    private Integer dayOfWeek;
    private Integer dayOfMonth;
    private Integer monthOfYear;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate nextOccurrence;
    private LocalDate lastGenerated;
    private boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
