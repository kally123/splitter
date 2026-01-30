package com.splitter.notification.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * User notification preferences DTO.
 */
@Builder
public record NotificationPreferenceDto(
    UUID userId,
    boolean emailExpenseAdded,
    boolean emailSettlementRequested,
    boolean emailSettlementConfirmed,
    boolean emailGroupInvitation,
    boolean emailReminders,
    boolean emailWeeklySummary,
    boolean pushEnabled,
    boolean pushExpenseAdded,
    boolean pushSettlementRequested,
    boolean pushSettlementConfirmed,
    boolean inAppEnabled,
    Instant updatedAt
) {}
