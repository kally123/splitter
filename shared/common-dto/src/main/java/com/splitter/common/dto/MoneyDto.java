package com.splitter.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Shared Money value object for representing monetary amounts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MoneyDto {

    /**
     * The monetary amount.
     */
    private BigDecimal amount;

    /**
     * ISO 4217 currency code (e.g., "USD", "EUR", "SEK").
     */
    private String currency;

    /**
     * Creates a MoneyDto with the specified amount and currency.
     */
    public static MoneyDto of(BigDecimal amount, String currency) {
        return MoneyDto.builder()
                .amount(amount)
                .currency(currency != null ? currency.toUpperCase() : "USD")
                .build();
    }

    /**
     * Creates a MoneyDto with USD as default currency.
     */
    public static MoneyDto usd(BigDecimal amount) {
        return of(amount, "USD");
    }

    /**
     * Creates a zero amount MoneyDto.
     */
    public static MoneyDto zero(String currency) {
        return of(BigDecimal.ZERO, currency);
    }
}
