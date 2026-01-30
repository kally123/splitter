package com.splitter.expense.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Recurring expense template for automatically generated expenses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("recurring_expenses")
public class RecurringExpense {

    @Id
    private UUID id;

    @Column("group_id")
    private UUID groupId;

    @Column("created_by")
    private UUID createdBy;

    @Column("description")
    private String description;

    @Column("amount")
    private BigDecimal amount;

    @Column("currency")
    @Builder.Default
    private String currency = "USD";

    @Column("category")
    private ExpenseCategory category;

    @Column("split_type")
    @Builder.Default
    private SplitType splitType = SplitType.EQUAL;

    @Column("splits")
    private String splitsJson;

    @Column("frequency")
    private RecurrenceFrequency frequency;

    @Column("interval_value")
    @Builder.Default
    private Integer intervalValue = 1;

    @Column("day_of_week")
    private Integer dayOfWeek;

    @Column("day_of_month")
    private Integer dayOfMonth;

    @Column("month_of_year")
    private Integer monthOfYear;

    @Column("start_date")
    private LocalDate startDate;

    @Column("end_date")
    private LocalDate endDate;

    @Column("next_occurrence")
    private LocalDate nextOccurrence;

    @Column("last_generated")
    private LocalDate lastGenerated;

    @Column("is_active")
    @Builder.Default
    private boolean active = true;

    @Column("is_deleted")
    @Builder.Default
    private boolean deleted = false;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;

    /**
     * Calculate the next occurrence date from a given date.
     */
    public LocalDate calculateNextOccurrence(LocalDate from) {
        return switch (frequency) {
            case DAILY -> from.plusDays(intervalValue);
            case WEEKLY -> from.plusWeeks(intervalValue);
            case BIWEEKLY -> from.plusWeeks(2L * intervalValue);
            case MONTHLY -> {
                LocalDate next = from.plusMonths(intervalValue);
                // Adjust for day of month if specified
                if (dayOfMonth != null) {
                    int maxDay = next.lengthOfMonth();
                    int targetDay = Math.min(dayOfMonth, maxDay);
                    yield next.withDayOfMonth(targetDay);
                }
                yield next;
            }
            case YEARLY -> {
                LocalDate next = from.plusYears(intervalValue);
                // Adjust for month and day if specified
                if (monthOfYear != null) {
                    next = next.withMonth(monthOfYear);
                    if (dayOfMonth != null) {
                        int maxDay = next.lengthOfMonth();
                        int targetDay = Math.min(dayOfMonth, maxDay);
                        next = next.withDayOfMonth(targetDay);
                    }
                }
                yield next;
            }
        };
    }

    /**
     * Check if the recurring expense has ended.
     */
    public boolean hasEnded() {
        if (endDate == null) {
            return false;
        }
        return LocalDate.now().isAfter(endDate);
    }
}
