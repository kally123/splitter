package com.splitter.payment.dto;

import com.splitter.payment.model.PaymentProvider;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddPaymentMethodRequest {
    
    @NotBlank(message = "Payment method token is required")
    private String paymentMethodToken;
    
    private PaymentProvider provider;
    
    private String email;
    
    private String name;
    
    private boolean setAsDefault;
}
