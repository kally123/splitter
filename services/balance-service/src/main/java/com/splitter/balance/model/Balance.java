package com.splitter.balance.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Balance entity representing the net balance between two users in a group.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("balances")
public class Balance {

    @Id
    private UUID id;

    @Column("group_id")
    private UUID groupId;

    @Column("from_user_id")
    private UUID fromUserId;

    @Column("to_user_id")
    private UUID toUserId;

    @Column("amount")
    private BigDecimal amount;

    @Column("currency")
    @Builder.Default
    private String currency = "USD";

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;

    /**
     * Check if this balance represents a debt (positive amount).
     */
    public boolean isDebt() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Get the absolute value of the amount.
     */
    public BigDecimal getAbsoluteAmount() {
        return amount != null ? amount.abs() : BigDecimal.ZERO;
    }
}
