package com.splitter.notification.dto;

/**
 * Request for updating notification preferences.
 */
public record UpdatePreferencesRequest(
    Boolean emailExpenseAdded,
    Boolean emailSettlementRequested,
    Boolean emailSettlementConfirmed,
    Boolean emailGroupInvitation,
    Boolean emailReminders,
    Boolean emailWeeklySummary,
    Boolean pushEnabled,
    Boolean pushExpenseAdded,
    Boolean pushSettlementRequested,
    Boolean pushSettlementConfirmed,
    Boolean inAppEnabled
) {}
