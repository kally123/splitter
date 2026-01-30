package com.splitter.payment.repository;

import com.splitter.payment.model.PaymentMethod;
import com.splitter.payment.model.PaymentProvider;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface PaymentMethodRepository extends ReactiveCrudRepository<PaymentMethod, UUID> {
    
    Flux<PaymentMethod> findByUserIdAndIsActiveTrue(UUID userId);
    
    Mono<PaymentMethod> findByUserIdAndIsDefaultTrueAndIsActiveTrue(UUID userId);
    
    Mono<PaymentMethod> findByUserIdAndProviderAndIsActiveTrue(UUID userId, PaymentProvider provider);
    
    Mono<PaymentMethod> findByProviderPaymentMethodId(String providerPaymentMethodId);
    
    @Modifying
    @Query("UPDATE payment_methods SET is_default = false WHERE user_id = :userId")
    Mono<Void> clearDefaultForUser(UUID userId);
    
    @Modifying
    @Query("UPDATE payment_methods SET is_active = false WHERE id = :id")
    Mono<Void> deactivate(UUID id);
}
