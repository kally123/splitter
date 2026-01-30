package com.splitter.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitter.payment.dto.*;
import com.splitter.payment.model.*;
import com.splitter.payment.repository.PaymentMethodRepository;
import com.splitter.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final List<PaymentProviderService> providerServices;
    private final ObjectMapper objectMapper;
    
    private Map<PaymentProvider, PaymentProviderService> getProviderMap() {
        return providerServices.stream()
            .filter(PaymentProviderService::isAvailable)
            .collect(Collectors.toMap(
                PaymentProviderService::getProvider,
                Function.identity()
            ));
    }
    
    public Mono<PaymentResponse> createPayment(CreatePaymentRequest request, UUID userId) {
        // Check for idempotency
        if (request.getIdempotencyKey() != null) {
            return paymentRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .map(this::toResponse)
                .switchIfEmpty(Mono.defer(() -> processNewPayment(request, userId)));
        }
        
        return processNewPayment(request, userId);
    }
    
    private Mono<PaymentResponse> processNewPayment(CreatePaymentRequest request, UUID userId) {
        PaymentProvider provider = request.getProvider() != null ? 
            request.getProvider() : PaymentProvider.STRIPE;
        
        PaymentProviderService providerService = getProviderMap().get(provider);
        if (providerService == null) {
            return Mono.error(new IllegalStateException("Payment provider not available: " + provider));
        }
        
        // Get user's payment method
        return paymentMethodRepository.findByUserIdAndProviderAndIsActiveTrue(userId, provider)
            .switchIfEmpty(Mono.error(new IllegalStateException("No payment method found for user")))
            .flatMap(paymentMethod -> {
                // Create payment record
                Payment payment = Payment.builder()
                    .id(UUID.randomUUID())
                    .fromUserId(request.getFromUserId())
                    .toUserId(request.getToUserId())
                    .settlementId(request.getSettlementId())
                    .groupId(request.getGroupId())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .status(PaymentStatus.PENDING)
                    .provider(provider)
                    .idempotencyKey(request.getIdempotencyKey())
                    .createdAt(Instant.now())
                    .build();
                
                return paymentRepository.save(payment)
                    .flatMap(savedPayment -> 
                        providerService.processPayment(
                            request, 
                            paymentMethod.getProviderCustomerId(), 
                            paymentMethod.getProviderPaymentMethodId()
                        )
                        .flatMap(result -> updatePaymentFromResult(savedPayment, result))
                    );
            })
            .map(this::toResponse);
    }
    
    private Mono<Payment> updatePaymentFromResult(Payment payment, PaymentResult result) {
        payment.setStatus(result.getStatus());
        payment.setProviderTransactionId(result.getProviderTransactionId());
        payment.setProviderFee(result.getProviderFee());
        payment.setFailureReason(result.getErrorMessage());
        
        if (result.getStatus() == PaymentStatus.COMPLETED) {
            payment.setCompletedAt(Instant.now());
        }
        
        payment.setUpdatedAt(Instant.now());
        return paymentRepository.save(payment);
    }
    
    public Mono<PaymentResponse> getPayment(UUID id) {
        return paymentRepository.findById(id)
            .map(this::toResponse);
    }
    
    public Flux<PaymentResponse> getUserPayments(UUID userId) {
        return paymentRepository.findByUserId(userId)
            .map(this::toResponse);
    }
    
    public Flux<PaymentResponse> getGroupPayments(UUID groupId) {
        return paymentRepository.findByGroupId(groupId)
            .map(this::toResponse);
    }
    
    public Mono<PaymentResponse> refundPayment(UUID paymentId, UUID userId) {
        return paymentRepository.findById(paymentId)
            .filter(payment -> payment.getFromUserId().equals(userId) || 
                              payment.getToUserId().equals(userId))
            .filter(payment -> payment.getStatus() == PaymentStatus.COMPLETED)
            .flatMap(payment -> {
                PaymentProviderService providerService = getProviderMap().get(payment.getProvider());
                if (providerService == null) {
                    return Mono.error(new IllegalStateException("Provider not available"));
                }
                
                return providerService.refundPayment(payment.getProviderTransactionId(), payment.getAmount())
                    .flatMap(result -> {
                        payment.setStatus(PaymentStatus.REFUNDED);
                        payment.setUpdatedAt(Instant.now());
                        return paymentRepository.save(payment);
                    });
            })
            .map(this::toResponse);
    }
    
    // Payment Methods
    
    public Flux<PaymentMethodResponse> getUserPaymentMethods(UUID userId) {
        return paymentMethodRepository.findByUserIdAndIsActiveTrue(userId)
            .map(this::toPaymentMethodResponse);
    }
    
    public Mono<PaymentMethodResponse> addPaymentMethod(AddPaymentMethodRequest request, UUID userId) {
        PaymentProvider provider = request.getProvider() != null ? 
            request.getProvider() : PaymentProvider.STRIPE;
        
        PaymentProviderService providerService = getProviderMap().get(provider);
        if (providerService == null) {
            return Mono.error(new IllegalStateException("Payment provider not available: " + provider));
        }
        
        // Get or create customer
        return paymentMethodRepository.findByUserIdAndProviderAndIsActiveTrue(userId, provider)
            .map(pm -> pm.getProviderCustomerId())
            .switchIfEmpty(providerService.createCustomer(userId, request.getEmail(), request.getName()))
            .flatMap(customerId -> 
                providerService.attachPaymentMethod(userId, customerId, request.getPaymentMethodToken())
            )
            .flatMap(paymentMethod -> {
                if (request.isSetAsDefault()) {
                    return paymentMethodRepository.clearDefaultForUser(userId)
                        .then(Mono.defer(() -> {
                            paymentMethod.setDefault(true);
                            return paymentMethodRepository.save(paymentMethod);
                        }));
                }
                return paymentMethodRepository.save(paymentMethod);
            })
            .map(this::toPaymentMethodResponse);
    }
    
    public Mono<Void> removePaymentMethod(UUID paymentMethodId, UUID userId) {
        return paymentMethodRepository.findById(paymentMethodId)
            .filter(pm -> pm.getUserId().equals(userId))
            .flatMap(pm -> paymentMethodRepository.deactivate(pm.getId()));
    }
    
    public Mono<PaymentMethodResponse> setDefaultPaymentMethod(UUID paymentMethodId, UUID userId) {
        return paymentMethodRepository.findById(paymentMethodId)
            .filter(pm -> pm.getUserId().equals(userId) && pm.isActive())
            .flatMap(pm -> 
                paymentMethodRepository.clearDefaultForUser(userId)
                    .then(Mono.defer(() -> {
                        pm.setDefault(true);
                        pm.setUpdatedAt(Instant.now());
                        return paymentMethodRepository.save(pm);
                    }))
            )
            .map(this::toPaymentMethodResponse);
    }
    
    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
            .id(payment.getId())
            .fromUserId(payment.getFromUserId())
            .toUserId(payment.getToUserId())
            .settlementId(payment.getSettlementId())
            .groupId(payment.getGroupId())
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .status(payment.getStatus().name())
            .provider(payment.getProvider().name())
            .providerTransactionId(payment.getProviderTransactionId())
            .providerFee(payment.getProviderFee())
            .failureReason(payment.getFailureReason())
            .createdAt(payment.getCreatedAt())
            .completedAt(payment.getCompletedAt())
            .build();
    }
    
    private PaymentMethodResponse toPaymentMethodResponse(PaymentMethod pm) {
        return PaymentMethodResponse.builder()
            .id(pm.getId())
            .provider(pm.getProvider().name())
            .type(pm.getType())
            .lastFour(pm.getLastFour())
            .brand(pm.getBrand())
            .expMonth(pm.getExpMonth())
            .expYear(pm.getExpYear())
            .isDefault(pm.isDefault())
            .createdAt(pm.getCreatedAt())
            .build();
    }
}
