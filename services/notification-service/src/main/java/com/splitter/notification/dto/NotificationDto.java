package com.splitter.notification.dto;

import com.splitter.notification.model.Notification;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Notification data transfer object.
 */
@Builder
public record NotificationDto(
    UUID id,
    UUID userId,
    Notification.NotificationType type,
    Notification.NotificationChannel channel,
    String title,
    String message,
    String data,
    String referenceType,
    UUID referenceId,
    Notification.NotificationStatus status,
    boolean read,
    Instant readAt,
    Instant sentAt,
    Instant createdAt
) {}
