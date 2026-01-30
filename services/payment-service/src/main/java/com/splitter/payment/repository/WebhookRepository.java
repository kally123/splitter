package com.splitter.payment.repository;

import com.splitter.payment.model.PaymentProvider;
import com.splitter.payment.model.Webhook;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface WebhookRepository extends ReactiveCrudRepository<Webhook, UUID> {
    
    Mono<Boolean> existsByEventId(String eventId);
    
    Flux<Webhook> findByProviderAndProcessedFalse(PaymentProvider provider);
    
    @Query("SELECT * FROM webhooks WHERE processed = false ORDER BY received_at ASC LIMIT :limit")
    Flux<Webhook> findUnprocessedWebhooks(int limit);
}
