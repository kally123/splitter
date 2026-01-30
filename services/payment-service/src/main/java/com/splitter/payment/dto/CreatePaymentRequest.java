package com.splitter.payment.dto;

import com.splitter.payment.model.PaymentProvider;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {
    
    @NotNull(message = "From user ID is required")
    private UUID fromUserId;
    
    @NotNull(message = "To user ID is required")
    private UUID toUserId;
    
    private UUID settlementId;
    
    private UUID groupId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    private String currency;
    
    private PaymentProvider provider;
    
    private String idempotencyKey;
    
    private String description;
}
