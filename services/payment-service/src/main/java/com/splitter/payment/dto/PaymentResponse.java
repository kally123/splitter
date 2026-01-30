package com.splitter.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private UUID id;
    private UUID fromUserId;
    private UUID toUserId;
    private UUID settlementId;
    private UUID groupId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String provider;
    private String providerTransactionId;
    private BigDecimal providerFee;
    private String clientSecret; // For 3D Secure etc.
    private String failureReason;
    private Instant createdAt;
    private Instant completedAt;
}
