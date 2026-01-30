package com.splitter.currency.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateResponse {
    
    private String baseCurrency;
    private String targetCurrency;
    private BigDecimal rate;
    private LocalDate rateDate;
    private String provider;
}
