package com.splitter.currency.repository;

import com.splitter.currency.model.Currency;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CurrencyRepository extends ReactiveCrudRepository<Currency, String> {
    
    Flux<Currency> findByIsActiveTrueOrderByDisplayOrderAsc();
    
    Mono<Currency> findByCodeAndIsActiveTrue(String code);
    
    Flux<Currency> findByCodeInAndIsActiveTrue(Iterable<String> codes);
}
