package com.splitter.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodResponse {
    private UUID id;
    private String provider;
    private String type;
    private String lastFour;
    private String brand;
    private Integer expMonth;
    private Integer expYear;
    private boolean isDefault;
    private Instant createdAt;
}
