package com.splitter.currency.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("currencies")
public class Currency {
    
    @Id
    private String code;
    
    private String name;
    
    private String symbol;
    
    @Column("decimal_places")
    private Integer decimalPlaces;
    
    @Column("is_active")
    private Boolean isActive;
    
    @Column("display_order")
    private Integer displayOrder;
}
