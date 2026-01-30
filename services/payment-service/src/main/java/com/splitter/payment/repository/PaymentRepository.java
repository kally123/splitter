package com.splitter.payment.repository;

import com.splitter.payment.model.Payment;
import com.splitter.payment.model.PaymentStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface PaymentRepository extends ReactiveCrudRepository<Payment, UUID> {
    
    Flux<Payment> findByFromUserId(UUID fromUserId);
    
    Flux<Payment> findByToUserId(UUID toUserId);
    
    Flux<Payment> findBySettlementId(UUID settlementId);
    
    Flux<Payment> findByGroupId(UUID groupId);
    
    Mono<Payment> findByIdempotencyKey(String idempotencyKey);
    
    Mono<Payment> findByProviderTransactionId(String providerTransactionId);
    
    @Query("SELECT * FROM payments WHERE from_user_id = :userId OR to_user_id = :userId ORDER BY created_at DESC")
    Flux<Payment> findByUserId(UUID userId);
    
    Flux<Payment> findByStatus(PaymentStatus status);
}
