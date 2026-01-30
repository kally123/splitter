package com.splitter.payment.service;

import com.splitter.payment.dto.CreatePaymentRequest;
import com.splitter.payment.dto.PaymentResult;
import com.splitter.payment.model.PaymentMethod;
import com.splitter.payment.model.PaymentProvider;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentProviderService {
    
    PaymentProvider getProvider();
    
    Mono<String> createCustomer(UUID userId, String email, String name);
    
    Mono<PaymentMethod> attachPaymentMethod(UUID userId, String providerCustomerId, String paymentMethodToken);
    
    Mono<PaymentResult> processPayment(CreatePaymentRequest request, String customerId, String paymentMethodId);
    
    Mono<PaymentResult> refundPayment(String transactionId, BigDecimal amount);
    
    Mono<Boolean> verifyWebhook(String payload, String signature);
    
    boolean isAvailable();
}
