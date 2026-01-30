package com.splitter.common.cache;

/**
 * Constants for cache names used across services.
 * Ensures consistent cache key naming.
 */
public final class CacheNames {
    
    private CacheNames() {
        // Utility class
    }
    
    // User related caches
    public static final String USER_PROFILE = "userProfile";
    public static final String USER_SESSION = "userSession";
    public static final String USER_BALANCES = "userBalances";
    public static final String USER_GROUPS = "userGroups";
    
    // Group related caches
    public static final String GROUP_DETAILS = "groupDetails";
    public static final String GROUP_SUMMARY = "groupSummary";
    public static final String GROUP_MEMBERS = "groupMembers";
    public static final String GROUP_BALANCES = "groupBalances";
    
    // Expense related caches
    public static final String EXPENSE_LIST = "expenseList";
    public static final String EXPENSE_DETAILS = "expenseDetails";
    
    // Currency related caches
    public static final String EXCHANGE_RATES = "exchangeRates";
    public static final String CURRENCY_LIST = "currencyList";
    
    // Analytics caches
    public static final String ANALYTICS_SUMMARY = "analyticsSummary";
    public static final String ANALYTICS_TREND = "analyticsTrend";
    public static final String ANALYTICS_CATEGORY = "analyticsCategory";
    
    // Settlement caches
    public static final String SETTLEMENT_SUGGESTIONS = "settlementSuggestions";
    public static final String PENDING_SETTLEMENTS = "pendingSettlements";
    
    // Static data
    public static final String STATIC_DATA = "staticData";
    public static final String CATEGORIES = "categories";
}
