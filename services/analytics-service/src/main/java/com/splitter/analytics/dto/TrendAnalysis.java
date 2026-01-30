package com.splitter.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendAnalysis {
    private LocalDate startDate;
    private LocalDate endDate;
    private String period; // daily, weekly, monthly
    
    private List<TrendPoint> spending;
    private List<TrendPoint> income; // For amounts owed to user
    
    private BigDecimal averageSpending;
    private BigDecimal spendingGrowthRate; // % change
    private String trend; // increasing, decreasing, stable
    
    private Forecast forecast;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private LocalDate date;
        private BigDecimal amount;
        private int count;
        private BigDecimal cumulativeAmount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Forecast {
        private BigDecimal nextPeriodEstimate;
        private BigDecimal nextMonthEstimate;
        private double confidence;
    }
}
