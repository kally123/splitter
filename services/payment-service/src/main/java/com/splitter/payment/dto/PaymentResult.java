package com.splitter.payment.dto;

import com.splitter.payment.model.PaymentProvider;
import com.splitter.payment.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResult {
    private PaymentStatus status;
    private PaymentProvider provider;
    private String providerTransactionId;
    private BigDecimal providerFee;
    private String clientSecret;
    private String errorMessage;
}
