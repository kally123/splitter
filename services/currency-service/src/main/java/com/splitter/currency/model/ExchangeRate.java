package com.splitter.currency.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("exchange_rates")
public class ExchangeRate {
    
    @Id
    private UUID id;
    
    @Column("base_currency")
    private String baseCurrency;
    
    @Column("target_currency")
    private String targetCurrency;
    
    private BigDecimal rate;
    
    @Column("rate_date")
    private LocalDate rateDate;
    
    private String provider;
    
    @Column("fetched_at")
    private Instant fetchedAt;
}
