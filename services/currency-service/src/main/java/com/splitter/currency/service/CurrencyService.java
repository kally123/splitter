package com.splitter.currency.service;

import com.splitter.currency.model.ConversionResult;
import com.splitter.currency.model.Currency;
import com.splitter.currency.model.ExchangeRate;
import com.splitter.currency.repository.CurrencyRepository;
import com.splitter.currency.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class CurrencyService {
    
    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateProvider exchangeRateProvider;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final String RATE_CACHE_PREFIX = "exchange:rate:";
    
    public Flux<Currency> getAllCurrencies() {
        return currencyRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }
    
    public Mono<Currency> getCurrency(String code) {
        return currencyRepository.findByCodeAndIsActiveTrue(code.toUpperCase());
    }
    
    public Mono<ConversionResult> convert(BigDecimal amount, String fromCurrency, 
                                          String toCurrency, LocalDate date) {
        String from = fromCurrency.toUpperCase();
        String to = toCurrency.toUpperCase();
        
        if (from.equals(to)) {
            return Mono.just(ConversionResult.builder()
                .originalAmount(amount)
                .convertedAmount(amount)
                .exchangeRate(BigDecimal.ONE)
                .fromCurrency(from)
                .toCurrency(to)
                .rateDate(date)
                .build());
        }
        
        return getExchangeRate(from, to, date)
            .map(rate -> {
                BigDecimal convertedAmount = amount.multiply(rate.getRate())
                    .setScale(2, RoundingMode.HALF_UP);
                    
                return ConversionResult.builder()
                    .originalAmount(amount)
                    .convertedAmount(convertedAmount)
                    .exchangeRate(rate.getRate())
                    .fromCurrency(from)
                    .toCurrency(to)
                    .rateDate(rate.getRateDate())
                    .provider(rate.getProvider())
                    .build();
            });
    }
    
    public Mono<ExchangeRate> getExchangeRate(String fromCurrency, String toCurrency, LocalDate date) {
        String cacheKey = buildCacheKey(fromCurrency, toCurrency, date);
        
        return getCachedRate(cacheKey)
            .switchIfEmpty(
                fetchAndCacheRate(fromCurrency, toCurrency, date, cacheKey)
            );
    }
    
    public Flux<ExchangeRate> getAllRatesForBase(String baseCurrency, LocalDate date) {
        return exchangeRateRepository.findAllRatesForBase(baseCurrency.toUpperCase(), date)
            .switchIfEmpty(
                exchangeRateProvider.getAllRates(baseCurrency.toUpperCase(), date)
                    .flatMap(this::saveRate)
            );
    }
    
    private Mono<ExchangeRate> getCachedRate(String cacheKey) {
        return redisTemplate.opsForValue().get(cacheKey)
            .flatMap(rateStr -> {
                try {
                    String[] parts = rateStr.split(":");
                    return Mono.just(ExchangeRate.builder()
                        .rate(new BigDecimal(parts[0]))
                        .provider(parts[1])
                        .build());
                } catch (Exception e) {
                    log.warn("Failed to parse cached rate: {}", rateStr);
                    return Mono.empty();
                }
            });
    }
    
    private Mono<ExchangeRate> fetchAndCacheRate(String from, String to, LocalDate date, String cacheKey) {
        // First try database
        return exchangeRateRepository.findRate(from, to, date)
            .switchIfEmpty(
                // If not in DB for exact date, try closest date
                exchangeRateRepository.findClosestRate(from, to, date)
            )
            .switchIfEmpty(
                // If still not found, fetch from provider
                exchangeRateProvider.getRate(from, to, date)
                    .flatMap(this::saveRate)
            )
            .flatMap(rate -> cacheRate(cacheKey, rate).thenReturn(rate));
    }
    
    private Mono<ExchangeRate> saveRate(ExchangeRate rate) {
        return exchangeRateRepository.save(rate);
    }
    
    private Mono<Boolean> cacheRate(String cacheKey, ExchangeRate rate) {
        String value = rate.getRate().toPlainString() + ":" + rate.getProvider();
        return redisTemplate.opsForValue().set(cacheKey, value, CACHE_TTL);
    }
    
    private String buildCacheKey(String from, String to, LocalDate date) {
        return RATE_CACHE_PREFIX + from + ":" + to + ":" + date;
    }
}
