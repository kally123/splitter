package com.splitter.currency.service;

import com.splitter.currency.model.ExchangeRate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface ExchangeRateProvider {
    
    Mono<ExchangeRate> getRate(String baseCurrency, String targetCurrency, LocalDate date);
    
    Flux<ExchangeRate> getAllRates(String baseCurrency, LocalDate date);
    
    String getProviderName();
    
    boolean isAvailable();
}
