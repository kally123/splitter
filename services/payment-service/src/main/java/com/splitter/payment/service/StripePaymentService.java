package com.splitter.payment.service;

import com.splitter.payment.dto.CreatePaymentRequest;
import com.splitter.payment.dto.PaymentResult;
import com.splitter.payment.model.PaymentMethod;
import com.splitter.payment.model.PaymentProvider;
import com.splitter.payment.model.PaymentStatus;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.Webhook;
import com.stripe.param.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class StripePaymentService implements PaymentProviderService {
    
    @Value("${stripe.api-key:}")
    private String apiKey;
    
    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;
    
    @Value("${stripe.enabled:false}")
    private boolean enabled;
    
    @PostConstruct
    public void init() {
        if (enabled && apiKey != null && !apiKey.isEmpty()) {
            Stripe.apiKey = apiKey;
            log.info("Stripe payment service initialized");
        }
    }
    
    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.STRIPE;
    }
    
    @Override
    public Mono<String> createCustomer(UUID userId, String email, String name) {
        return Mono.fromCallable(() -> {
            CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(email)
                .setName(name)
                .putMetadata("user_id", userId.toString())
                .build();
            
            Customer customer = Customer.create(params);
            log.info("Created Stripe customer: {} for user: {}", customer.getId(), userId);
            return customer.getId();
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<PaymentMethod> attachPaymentMethod(UUID userId, String providerCustomerId, String paymentMethodToken) {
        return Mono.fromCallable(() -> {
            // Attach payment method to customer
            com.stripe.model.PaymentMethod stripePaymentMethod = 
                com.stripe.model.PaymentMethod.retrieve(paymentMethodToken);
            
            PaymentMethodAttachParams attachParams = PaymentMethodAttachParams.builder()
                .setCustomer(providerCustomerId)
                .build();
            
            stripePaymentMethod = stripePaymentMethod.attach(attachParams);
            
            // Build our PaymentMethod entity
            PaymentMethod.PaymentMethodBuilder builder = PaymentMethod.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .provider(PaymentProvider.STRIPE)
                .providerCustomerId(providerCustomerId)
                .providerPaymentMethodId(stripePaymentMethod.getId())
                .isActive(true);
            
            // Extract card details if it's a card
            if (stripePaymentMethod.getCard() != null) {
                com.stripe.model.PaymentMethod.Card card = stripePaymentMethod.getCard();
                builder.type("card")
                    .lastFour(card.getLast4())
                    .brand(card.getBrand())
                    .expMonth(card.getExpMonth().intValue())
                    .expYear(card.getExpYear().intValue());
            }
            
            log.info("Attached payment method: {} for user: {}", stripePaymentMethod.getId(), userId);
            return builder.build();
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<PaymentResult> processPayment(CreatePaymentRequest request, String customerId, String paymentMethodId) {
        return Mono.fromCallable(() -> {
            // Convert amount to cents for Stripe
            long amountInCents = request.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValue();
            
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(request.getCurrency().toLowerCase())
                .setCustomer(customerId)
                .setPaymentMethod(paymentMethodId)
                .setConfirm(true)
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                        .build()
                )
                .putMetadata("from_user_id", request.getFromUserId().toString())
                .putMetadata("to_user_id", request.getToUserId().toString())
                .putMetadata("settlement_id", request.getSettlementId() != null ? 
                    request.getSettlementId().toString() : "")
                .build();
            
            if (request.getIdempotencyKey() != null) {
                // Use idempotency key for safe retries
            }
            
            PaymentIntent paymentIntent = PaymentIntent.create(params);
            
            PaymentResult.PaymentResultBuilder resultBuilder = PaymentResult.builder()
                .providerTransactionId(paymentIntent.getId())
                .provider(PaymentProvider.STRIPE);
            
            switch (paymentIntent.getStatus()) {
                case "succeeded":
                    resultBuilder.status(PaymentStatus.COMPLETED);
                    break;
                case "requires_action":
                    resultBuilder.status(PaymentStatus.REQUIRES_ACTION)
                        .clientSecret(paymentIntent.getClientSecret());
                    break;
                case "processing":
                    resultBuilder.status(PaymentStatus.PROCESSING);
                    break;
                default:
                    resultBuilder.status(PaymentStatus.FAILED)
                        .errorMessage("Unexpected status: " + paymentIntent.getStatus());
            }
            
            // Calculate fee (Stripe typically charges 2.9% + 30¢)
            if (paymentIntent.getLatestCharge() != null) {
                Charge charge = Charge.retrieve(paymentIntent.getLatestCharge());
                if (charge.getBalanceTransaction() != null) {
                    BalanceTransaction bt = BalanceTransaction.retrieve(charge.getBalanceTransaction());
                    resultBuilder.providerFee(BigDecimal.valueOf(bt.getFee()).divide(BigDecimal.valueOf(100)));
                }
            }
            
            log.info("Processed payment: {} with status: {}", paymentIntent.getId(), paymentIntent.getStatus());
            return resultBuilder.build();
        }).subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(StripeException.class, e -> {
            log.error("Stripe payment failed: {}", e.getMessage());
            return Mono.just(PaymentResult.builder()
                .status(PaymentStatus.FAILED)
                .errorMessage(e.getMessage())
                .build());
        });
    }
    
    @Override
    public Mono<PaymentResult> refundPayment(String transactionId, BigDecimal amount) {
        return Mono.fromCallable(() -> {
            RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder()
                .setPaymentIntent(transactionId);
            
            if (amount != null) {
                paramsBuilder.setAmount(amount.multiply(BigDecimal.valueOf(100)).longValue());
            }
            
            Refund refund = Refund.create(paramsBuilder.build());
            
            log.info("Created refund: {} for payment: {}", refund.getId(), transactionId);
            
            return PaymentResult.builder()
                .providerTransactionId(refund.getId())
                .provider(PaymentProvider.STRIPE)
                .status(refund.getStatus().equals("succeeded") ? 
                    PaymentStatus.REFUNDED : PaymentStatus.PROCESSING)
                .build();
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<Boolean> verifyWebhook(String payload, String signature) {
        return Mono.fromCallable(() -> {
            try {
                Webhook.constructEvent(payload, signature, webhookSecret);
                return true;
            } catch (Exception e) {
                log.warn("Webhook verification failed: {}", e.getMessage());
                return false;
            }
        });
    }
    
    @Override
    public boolean isAvailable() {
        return enabled && apiKey != null && !apiKey.isEmpty();
    }
}
