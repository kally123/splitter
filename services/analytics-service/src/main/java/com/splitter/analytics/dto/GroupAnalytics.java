package com.splitter.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupAnalytics {
    private UUID groupId;
    private String groupName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String currency;
    
    private BigDecimal totalExpenses;
    private int expenseCount;
    private int memberCount;
    private BigDecimal averageExpensePerMember;
    
    private List<MemberContribution> memberContributions;
    private List<CategoryBreakdown> byCategory;
    private List<MonthlyGroupSpending> monthlyTrend;
    private List<SettlementSummary> pendingSettlements;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberContribution {
        private UUID userId;
        private String userName;
        private BigDecimal paidAmount;
        private BigDecimal shareAmount;
        private BigDecimal balance;
        private int expenseCount;
        private double contributionPercentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyGroupSpending {
        private int year;
        private int month;
        private BigDecimal amount;
        private int count;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettlementSummary {
        private UUID fromUserId;
        private String fromUserName;
        private UUID toUserId;
        private String toUserName;
        private BigDecimal amount;
    }
}
