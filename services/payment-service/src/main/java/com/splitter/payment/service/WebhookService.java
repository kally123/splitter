package com.splitter.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitter.payment.model.PaymentProvider;
import com.splitter.payment.model.PaymentStatus;
import com.splitter.payment.model.Webhook;
import com.splitter.payment.repository.PaymentRepository;
import com.splitter.payment.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
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
public class WebhookService {
    
    private final WebhookRepository webhookRepository;
    private final PaymentRepository paymentRepository;
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
    
    public Mono<Void> handleStripeWebhook(String payload, String signature) {
        PaymentProviderService stripeService = getProviderMap().get(PaymentProvider.STRIPE);
        if (stripeService == null) {
            return Mono.error(new IllegalStateException("Stripe not available"));
        }
        
        return stripeService.verifyWebhook(payload, signature)
            .flatMap(valid -> {
                if (!valid) {
                    return Mono.error(new SecurityException("Invalid webhook signature"));
                }
                
                try {
                    JsonNode json = objectMapper.readTree(payload);
                    String eventId = json.get("id").asText();
                    String eventType = json.get("type").asText();
                    
                    // Check for duplicate
                    return webhookRepository.existsByEventId(eventId)
                        .flatMap(exists -> {
                            if (exists) {
                                log.info("Duplicate webhook event: {}", eventId);
                                return Mono.empty();
                            }
                            
                            Webhook webhook = Webhook.builder()
                                .id(UUID.randomUUID())
                                .provider(PaymentProvider.STRIPE)
                                .eventId(eventId)
                                .eventType(eventType)
                                .payloadJson(payload)
                                .processed(false)
                                .receivedAt(Instant.now())
                                .build();
                            
                            return webhookRepository.save(webhook)
                                .flatMap(this::processWebhook);
                        });
                } catch (Exception e) {
                    return Mono.error(e);
                }
            });
    }
    
    private Mono<Void> processWebhook(Webhook webhook) {
        return Mono.defer(() -> {
            try {
                JsonNode json = objectMapper.readTree(webhook.getPayloadJson());
                String eventType = webhook.getEventType();
                
                return switch (eventType) {
                    case "payment_intent.succeeded" -> handlePaymentSucceeded(json);
                    case "payment_intent.payment_failed" -> handlePaymentFailed(json);
                    case "charge.refunded" -> handleRefund(json);
                    default -> {
                        log.debug("Unhandled webhook event type: {}", eventType);
                        yield Mono.empty();
                    }
                };
            } catch (Exception e) {
                log.error("Failed to process webhook: {}", webhook.getId(), e);
                webhook.setProcessError(e.getMessage());
                return webhookRepository.save(webhook).then();
            }
        }).then(markWebhookProcessed(webhook));
    }
    
    private Mono<Void> handlePaymentSucceeded(JsonNode json) {
        String paymentIntentId = json.at("/data/object/id").asText();
        
        return paymentRepository.findByProviderTransactionId(paymentIntentId)
            .flatMap(payment -> {
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setCompletedAt(Instant.now());
                payment.setUpdatedAt(Instant.now());
                return paymentRepository.save(payment);
            })
            .doOnSuccess(p -> log.info("Payment succeeded via webhook: {}", paymentIntentId))
            .then();
    }
    
    private Mono<Void> handlePaymentFailed(JsonNode json) {
        String paymentIntentId = json.at("/data/object/id").asText();
        String failureMessage = json.at("/data/object/last_payment_error/message").asText();
        
        return paymentRepository.findByProviderTransactionId(paymentIntentId)
            .flatMap(payment -> {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(failureMessage);
                payment.setUpdatedAt(Instant.now());
                return paymentRepository.save(payment);
            })
            .doOnSuccess(p -> log.info("Payment failed via webhook: {}", paymentIntentId))
            .then();
    }
    
    private Mono<Void> handleRefund(JsonNode json) {
        String paymentIntentId = json.at("/data/object/payment_intent").asText();
        
        return paymentRepository.findByProviderTransactionId(paymentIntentId)
            .flatMap(payment -> {
                payment.setStatus(PaymentStatus.REFUNDED);
                payment.setUpdatedAt(Instant.now());
                return paymentRepository.save(payment);
            })
            .doOnSuccess(p -> log.info("Payment refunded via webhook: {}", paymentIntentId))
            .then();
    }
    
    private Mono<Void> markWebhookProcessed(Webhook webhook) {
        webhook.setProcessed(true);
        webhook.setProcessedAt(Instant.now());
        return webhookRepository.save(webhook).then();
    }
    
    @Scheduled(fixedDelay = 60000) // Every minute
    public void retryFailedWebhooks() {
        webhookRepository.findUnprocessedWebhooks(10)
            .flatMap(this::processWebhook)
            .subscribe();
    }
}
