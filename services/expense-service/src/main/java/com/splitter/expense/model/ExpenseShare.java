package com.splitter.expense.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ExpenseShare entity representing a participant's share in an expense.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("expense_shares")
public class ExpenseShare {

    @Id
    private UUID id;

    @Column("expense_id")
    private UUID expenseId;

    @Column("user_id")
    private UUID userId;

    @Column("share_amount")
    private BigDecimal shareAmount;

    @Column("share_percentage")
    private BigDecimal sharePercentage;

    @Column("share_units")
    private Integer shareUnits;

    @Column("is_paid")
    @Builder.Default
    private boolean paid = false;

    /**
     * Calculate owed amount (share amount minus any payments made).
     * For now, this equals share amount as payments are tracked separately.
     */
    public BigDecimal getOwedAmount() {
        return shareAmount;
    }
}
