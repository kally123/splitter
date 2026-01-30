package com.splitter.settlement.model;

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
 * Settlement entity representing a payment between users.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("settlements")
public class Settlement {

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

    @Column("payment_method")
    private PaymentMethod paymentMethod;

    @Column("status")
    @Builder.Default
    private SettlementStatus status = SettlementStatus.PENDING;

    @Column("settlement_date")
    private LocalDate settlementDate;

    @Column("notes")
    private String notes;

    @Column("external_reference")
    private String externalReference;

    @Column("created_by")
    private UUID createdBy;

    @Column("confirmed_by")
    private UUID confirmedBy;

    @Column("confirmed_at")
    private Instant confirmedAt;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;

    /**
     * Payment methods.
     */
    public enum PaymentMethod {
        CASH,
        BANK_TRANSFER,
        VENMO,
        PAYPAL,
        ZELLE,
        CREDIT_CARD,
        CHECK,
        OTHER
    }

    /**
     * Settlement statuses.
     */
    public enum SettlementStatus {
        PENDING,
        CONFIRMED,
        REJECTED,
        CANCELLED
    }
}
