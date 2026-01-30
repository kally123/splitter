package com.splitter.currency.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.splitter.currency.model.ExchangeRate;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class OpenExchangeRatesProvider implements ExchangeRateProvider {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${currency.provider.openexchangerates.api-key:}")
    private String apiKey;
    
    @Value("${currency.provider.openexchangerates.base-url:https://openexchangerates.org/api}")
    private String baseUrl;
    
    @Override
    public Mono<ExchangeRate> getRate(String baseCurrency, String targetCurrency, LocalDate date) {
        if (!isAvailable()) {
            return Mono.error(new IllegalStateException("OpenExchangeRates API key not configured"));
        }
        
        return getAllRates(baseCurrency, date)
            .filter(rate -> rate.getTargetCurrency().equals(targetCurrency))
            .next()
            .switchIfEmpty(Mono.error(new IllegalArgumentException(
                "Exchange rate not found for " + baseCurrency + " to " + targetCurrency)));
    }
    
    @Override
    public Flux<ExchangeRate> getAllRates(String baseCurrency, LocalDate date) {
        if (!isAvailable()) {
            return Flux.error(new IllegalStateException("OpenExchangeRates API key not configured"));
        }
        
        WebClient client = webClientBuilder.baseUrl(baseUrl).build();
        
        String endpoint = date.equals(LocalDate.now()) ? "/latest.json" : "/historical/" + date + ".json";
        
        return client.get()
            .uri(uriBuilder -> uriBuilder
                .path(endpoint)
                .queryParam("app_id", apiKey)
                .queryParam("base", baseCurrency)
                .build())
            .retrieve()
            .bodyToMono(OpenExchangeRatesResponse.class)
            .doOnError(e -> log.error("Failed to fetch exchange rates from OpenExchangeRates", e))
            .flatMapMany(response -> Flux.fromIterable(response.getRates().entrySet())
                .map(entry -> ExchangeRate.builder()
                    .id(UUID.randomUUID())
                    .baseCurrency(baseCurrency)
                    .targetCurrency(entry.getKey())
                    .rate(entry.getValue())
                    .rateDate(date)
                    .provider(getProviderName())
                    .fetchedAt(Instant.now())
                    .build()));
    }
    
    @Override
    public String getProviderName() {
        return "openexchangerates";
    }
    
    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }
    
    @Data
    private static class OpenExchangeRatesResponse {
        private String disclaimer;
        private String license;
        private Long timestamp;
        private String base;
        private Map<String, BigDecimal> rates;
    }
}
