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
 * Expense entity representing a shared expense.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("expenses")
public class Expense {

    @Id
    private UUID id;

    @Column("group_id")
    private UUID groupId;

    @Column("description")
    private String description;

    @Column("amount")
    private BigDecimal amount;

    @Column("currency")
    @Builder.Default
    private String currency = "USD";

    @Column("paid_by")
    private UUID paidBy;

    @Column("split_type")
    @Builder.Default
    private SplitType splitType = SplitType.EQUAL;

    @Column("category")
    private ExpenseCategory category;

    @Column("expense_date")
    private LocalDate expenseDate;

    @Column("notes")
    private String notes;

    @Column("receipt_url")
    private String receiptUrl;

    @Column("created_by")
    private UUID createdBy;

    @Column("is_deleted")
    @Builder.Default
    private boolean deleted = false;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;

    @Column("deleted_at")
    private Instant deletedAt;

    /**
     * Types of expense splits.
     */
    public enum SplitType {
        EQUAL,       // Split equally among all participants
        EXACT,       // Specify exact amounts for each participant
        PERCENTAGE,  // Specify percentage for each participant
        SHARES       // Specify shares for each participant
    }

    /**
     * Expense categories.
     */
    public enum ExpenseCategory {
        FOOD_AND_DRINK,
        GROCERIES,
        SHOPPING,
        ENTERTAINMENT,
        TRANSPORTATION,
        UTILITIES,
        RENT,
        HEALTHCARE,
        EDUCATION,
        TRAVEL,
        SPORTS,
        GIFTS,
        OTHER
    }
}
