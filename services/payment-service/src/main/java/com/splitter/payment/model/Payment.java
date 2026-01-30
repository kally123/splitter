package com.splitter.payment.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("payments")
public class Payment {
    
    @Id
    private UUID id;
    
    @Column("from_user_id")
    private UUID fromUserId;
    
    @Column("to_user_id")
    private UUID toUserId;
    
    @Column("settlement_id")
    private UUID settlementId;
    
    @Column("group_id")
    private UUID groupId;
    
    @Column("amount")
    private BigDecimal amount;
    
    @Column("currency")
    private String currency;
    
    @Column("status")
    private PaymentStatus status;
    
    @Column("provider")
    private PaymentProvider provider;
    
    @Column("provider_transaction_id")
    private String providerTransactionId;
    
    @Column("provider_fee")
    private BigDecimal providerFee;
    
    @Column("idempotency_key")
    private String idempotencyKey;
    
    @Column("metadata")
    private String metadataJson;
    
    @Column("failure_reason")
    private String failureReason;
    
    @CreatedDate
    @Column("created_at")
    private Instant createdAt;
    
    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;
    
    @Column("completed_at")
    private Instant completedAt;
}
