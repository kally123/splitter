package com.splitter.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpendingSummary {
    private UUID userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String currency;
    
    private BigDecimal totalSpent;
    private BigDecimal totalOwed;
    private BigDecimal totalOwedToYou;
    private BigDecimal netBalance;
    
    private int expenseCount;
    private int groupCount;
    private int settledCount;
    
    private BigDecimal averageExpense;
    private BigDecimal largestExpense;
    
    private List<CategoryBreakdown> byCategory;
    private List<GroupBreakdown> byGroup;
    private List<DailySpending> dailyTrend;
    private List<MonthlySpending> monthlyTrend;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CategoryBreakdown {
    private String category;
    private BigDecimal amount;
    private int count;
    private double percentage;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class GroupBreakdown {
    private UUID groupId;
    private String groupName;
    private BigDecimal amount;
    private int count;
    private double percentage;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class DailySpending {
    private LocalDate date;
    private BigDecimal amount;
    private int count;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class MonthlySpending {
    private int year;
    private int month;
    private BigDecimal amount;
    private int count;
}
