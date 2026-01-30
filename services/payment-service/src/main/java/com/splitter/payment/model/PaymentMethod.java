package com.splitter.payment.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("payment_methods")
public class PaymentMethod {
    
    @Id
    private UUID id;
    
    @Column("user_id")
    private UUID userId;
    
    @Column("provider")
    private PaymentProvider provider;
    
    @Column("provider_customer_id")
    private String providerCustomerId;
    
    @Column("provider_payment_method_id")
    private String providerPaymentMethodId;
    
    @Column("type")
    private String type; // card, bank_account, etc.
    
    @Column("last_four")
    private String lastFour;
    
    @Column("brand")
    private String brand; // visa, mastercard, etc.
    
    @Column("exp_month")
    private Integer expMonth;
    
    @Column("exp_year")
    private Integer expYear;
    
    @Column("is_default")
    private boolean isDefault;
    
    @Column("is_active")
    private boolean isActive;
    
    @CreatedDate
    @Column("created_at")
    private Instant createdAt;
    
    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;
}
