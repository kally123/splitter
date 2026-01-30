package com.splitter.currency.controller;

import com.splitter.currency.dto.ConversionRequest;
import com.splitter.currency.dto.ConversionResponse;
import com.splitter.currency.dto.CurrencyResponse;
import com.splitter.currency.dto.ExchangeRateResponse;
import com.splitter.currency.model.Currency;
import com.splitter.currency.model.ExchangeRate;
import com.splitter.currency.service.CurrencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/currencies")
@RequiredArgsConstructor
public class CurrencyController {
    
    private final CurrencyService currencyService;
    
    @GetMapping
    public Flux<CurrencyResponse> getAllCurrencies() {
        return currencyService.getAllCurrencies()
            .map(this::toCurrencyResponse);
    }
    
    @GetMapping("/{code}")
    public Mono<ResponseEntity<CurrencyResponse>> getCurrency(@PathVariable String code) {
        return currencyService.getCurrency(code)
            .map(this::toCurrencyResponse)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/convert")
    public Mono<ConversionResponse> convert(
            @RequestParam BigDecimal amount,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate rateDate = date != null ? date : LocalDate.now();
        
        return currencyService.convert(amount, from, to, rateDate)
            .map(result -> ConversionResponse.builder()
                .originalAmount(result.getOriginalAmount())
                .convertedAmount(result.getConvertedAmount())
                .exchangeRate(result.getExchangeRate())
                .fromCurrency(result.getFromCurrency())
                .toCurrency(result.getToCurrency())
                .rateDate(result.getRateDate())
                .provider(result.getProvider())
                .build());
    }
    
    @PostMapping("/convert")
    public Mono<ConversionResponse> convertPost(@Valid @RequestBody ConversionRequest request) {
        LocalDate rateDate = request.getDate() != null ? request.getDate() : LocalDate.now();
        
        return currencyService.convert(
                request.getAmount(), 
                request.getFromCurrency(), 
                request.getToCurrency(), 
                rateDate)
            .map(result -> ConversionResponse.builder()
                .originalAmount(result.getOriginalAmount())
                .convertedAmount(result.getConvertedAmount())
                .exchangeRate(result.getExchangeRate())
                .fromCurrency(result.getFromCurrency())
                .toCurrency(result.getToCurrency())
                .rateDate(result.getRateDate())
                .provider(result.getProvider())
                .build());
    }
    
    @GetMapping("/rates")
    public Flux<ExchangeRateResponse> getExchangeRates(
            @RequestParam(defaultValue = "USD") String base,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate rateDate = date != null ? date : LocalDate.now();
        
        return currencyService.getAllRatesForBase(base, rateDate)
            .map(this::toExchangeRateResponse);
    }
    
    @GetMapping("/rates/{from}/{to}")
    public Mono<ExchangeRateResponse> getExchangeRate(
            @PathVariable String from,
            @PathVariable String to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate rateDate = date != null ? date : LocalDate.now();
        
        return currencyService.getExchangeRate(from, to, rateDate)
            .map(this::toExchangeRateResponse);
    }
    
    private CurrencyResponse toCurrencyResponse(Currency currency) {
        return CurrencyResponse.builder()
            .code(currency.getCode())
            .name(currency.getName())
            .symbol(currency.getSymbol())
            .decimalPlaces(currency.getDecimalPlaces())
            .build();
    }
    
    private ExchangeRateResponse toExchangeRateResponse(ExchangeRate rate) {
        return ExchangeRateResponse.builder()
            .baseCurrency(rate.getBaseCurrency())
            .targetCurrency(rate.getTargetCurrency())
            .rate(rate.getRate())
            .rateDate(rate.getRateDate())
            .provider(rate.getProvider())
            .build();
    }
}
