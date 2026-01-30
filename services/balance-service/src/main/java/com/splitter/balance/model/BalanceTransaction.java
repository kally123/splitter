package com.splitter.balance.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * BalanceTransaction entity representing a single balance change event.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("balance_transactions")
public class BalanceTransaction {

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
    private String currency;

    @Column("transaction_type")
    private TransactionType transactionType;

    @Column("reference_id")
    private UUID referenceId;

    @Column("description")
    private String description;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    /**
     * Types of balance transactions.
     */
    public enum TransactionType {
        EXPENSE,        // From expense creation
        SETTLEMENT,     // From payment/settlement
        ADJUSTMENT      // Manual adjustment
    }
}
