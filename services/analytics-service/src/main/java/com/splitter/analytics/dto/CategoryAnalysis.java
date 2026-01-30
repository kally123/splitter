package com.splitter.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAnalysis {
    private String currency;
    private BigDecimal totalSpent;
    private List<CategoryDetail> categories;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDetail {
        private String category;
        private String categoryIcon;
        private BigDecimal amount;
        private int count;
        private double percentage;
        private BigDecimal averageAmount;
        private BigDecimal trend; // % change from previous period
        private List<SubcategoryDetail> subcategories;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubcategoryDetail {
        private String name;
        private BigDecimal amount;
        private int count;
    }
}
