package com.splitter.currency.repository;

import com.splitter.currency.model.ExchangeRate;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface ExchangeRateRepository extends ReactiveCrudRepository<ExchangeRate, UUID> {
    
    @Query("""
        SELECT * FROM exchange_rates 
        WHERE base_currency = :baseCurrency 
        AND target_currency = :targetCurrency 
        AND rate_date = :date
        ORDER BY fetched_at DESC
        LIMIT 1
        """)
    Mono<ExchangeRate> findRate(String baseCurrency, String targetCurrency, LocalDate date);
    
    @Query("""
        SELECT * FROM exchange_rates 
        WHERE base_currency = :baseCurrency 
        AND target_currency = :targetCurrency 
        AND rate_date <= :date
        ORDER BY rate_date DESC
        LIMIT 1
        """)
    Mono<ExchangeRate> findClosestRate(String baseCurrency, String targetCurrency, LocalDate date);
    
    @Query("""
        SELECT * FROM exchange_rates 
        WHERE base_currency = :baseCurrency 
        AND rate_date = :date
        """)
    Flux<ExchangeRate> findAllRatesForBase(String baseCurrency, LocalDate date);
    
    @Query("""
        DELETE FROM exchange_rates 
        WHERE rate_date < :beforeDate
        """)
    Mono<Void> deleteOlderThan(LocalDate beforeDate);
}
