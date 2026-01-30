package com.splitter.notification.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Notification entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("notifications")
public class Notification {

    @Id
    private UUID id;

    @Column("user_id")
    private UUID userId;

    @Column("notification_type")
    private NotificationType type;

    @Column("channel")
    private NotificationChannel channel;

    @Column("title")
    private String title;

    @Column("message")
    private String message;

    @Column("data")
    private String data; // JSON data

    @Column("reference_type")
    private String referenceType;

    @Column("reference_id")
    private UUID referenceId;

    @Column("status")
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column("is_read")
    @Builder.Default
    private boolean read = false;

    @Column("read_at")
    private Instant readAt;

    @Column("sent_at")
    private Instant sentAt;

    @Column("failure_reason")
    private String failureReason;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    /**
     * Notification types.
     */
    public enum NotificationType {
        EXPENSE_ADDED,
        EXPENSE_UPDATED,
        EXPENSE_DELETED,
        SETTLEMENT_REQUESTED,
        SETTLEMENT_CONFIRMED,
        SETTLEMENT_REJECTED,
        GROUP_INVITATION,
        GROUP_MEMBER_JOINED,
        GROUP_MEMBER_LEFT,
        REMINDER,
        SYSTEM
    }

    /**
     * Notification channels.
     */
    public enum NotificationChannel {
        IN_APP,
        EMAIL,
        PUSH
    }

    /**
     * Notification statuses.
     */
    public enum NotificationStatus {
        PENDING,
        SENT,
        DELIVERED,
        FAILED
    }
}
