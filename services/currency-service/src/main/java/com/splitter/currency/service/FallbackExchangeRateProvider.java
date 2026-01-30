package com.splitter.currency.service;

import com.splitter.currency.model.ExchangeRate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Fallback provider with static exchange rates for when external APIs are unavailable.
 * These rates are approximate and should only be used as a last resort.
 */
@Component
@Slf4j
public class FallbackExchangeRateProvider implements ExchangeRateProvider {
    
    // Approximate rates relative to USD as of 2024
    private static final Map<String, BigDecimal> USD_RATES = Map.ofEntries(
        Map.entry("EUR", new BigDecimal("0.92")),
        Map.entry("GBP", new BigDecimal("0.79")),
        Map.entry("JPY", new BigDecimal("149.50")),
        Map.entry("CAD", new BigDecimal("1.36")),
        Map.entry("AUD", new BigDecimal("1.53")),
        Map.entry("CHF", new BigDecimal("0.88")),
        Map.entry("CNY", new BigDecimal("7.24")),
        Map.entry("INR", new BigDecimal("83.12")),
        Map.entry("MXN", new BigDecimal("17.15")),
        Map.entry("BRL", new BigDecimal("4.97")),
        Map.entry("KRW", new BigDecimal("1330.50")),
        Map.entry("SGD", new BigDecimal("1.34")),
        Map.entry("HKD", new BigDecimal("7.82")),
        Map.entry("NOK", new BigDecimal("10.65")),
        Map.entry("SEK", new BigDecimal("10.42")),
        Map.entry("DKK", new BigDecimal("6.89")),
        Map.entry("NZD", new BigDecimal("1.64")),
        Map.entry("ZAR", new BigDecimal("18.75")),
        Map.entry("RUB", new BigDecimal("89.50")),
        Map.entry("TRY", new BigDecimal("30.25")),
        Map.entry("PLN", new BigDecimal("4.02")),
        Map.entry("THB", new BigDecimal("35.45")),
        Map.entry("IDR", new BigDecimal("15650.00")),
        Map.entry("PHP", new BigDecimal("55.75")),
        Map.entry("CZK", new BigDecimal("22.85")),
        Map.entry("ILS", new BigDecimal("3.65")),
        Map.entry("AED", new BigDecimal("3.67")),
        Map.entry("SAR", new BigDecimal("3.75")),
        Map.entry("MYR", new BigDecimal("4.72")),
        Map.entry("HUF", new BigDecimal("355.50")),
        Map.entry("USD", BigDecimal.ONE)
    );
    
    @Override
    public Mono<ExchangeRate> getRate(String baseCurrency, String targetCurrency, LocalDate date) {
        log.warn("Using fallback exchange rates for {} to {}", baseCurrency, targetCurrency);
        
        try {
            BigDecimal rate = calculateRate(baseCurrency, targetCurrency);
            return Mono.just(ExchangeRate.builder()
                .id(UUID.randomUUID())
                .baseCurrency(baseCurrency)
                .targetCurrency(targetCurrency)
                .rate(rate)
                .rateDate(date)
                .provider(getProviderName())
                .fetchedAt(Instant.now())
                .build());
        } catch (Exception e) {
            return Mono.error(new IllegalArgumentException(
                "Unsupported currency pair: " + baseCurrency + " to " + targetCurrency));
        }
    }
    
    @Override
    public Flux<ExchangeRate> getAllRates(String baseCurrency, LocalDate date) {
        log.warn("Using fallback exchange rates for base currency {}", baseCurrency);
        
        return Flux.fromIterable(USD_RATES.keySet())
            .filter(currency -> !currency.equals(baseCurrency))
            .flatMap(targetCurrency -> getRate(baseCurrency, targetCurrency, date));
    }
    
    @Override
    public String getProviderName() {
        return "fallback";
    }
    
    @Override
    public boolean isAvailable() {
        return true; // Always available as fallback
    }
    
    private BigDecimal calculateRate(String from, String to) {
        BigDecimal fromToUsd = USD_RATES.get(from);
        BigDecimal toToUsd = USD_RATES.get(to);
        
        if (fromToUsd == null || toToUsd == null) {
            throw new IllegalArgumentException("Unsupported currency");
        }
        
        // Convert: from -> USD -> to
        // rate = toToUsd / fromToUsd
        return toToUsd.divide(fromToUsd, 6, RoundingMode.HALF_UP);
    }
}
