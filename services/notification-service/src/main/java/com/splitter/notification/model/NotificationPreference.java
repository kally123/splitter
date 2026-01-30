package com.splitter.notification.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * User notification preferences.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("notification_preferences")
public class NotificationPreference {

    @Id
    private UUID id;

    @Column("user_id")
    private UUID userId;

    // Email preferences
    @Column("email_expense_added")
    @Builder.Default
    private boolean emailExpenseAdded = true;

    @Column("email_settlement_requested")
    @Builder.Default
    private boolean emailSettlementRequested = true;

    @Column("email_settlement_confirmed")
    @Builder.Default
    private boolean emailSettlementConfirmed = true;

    @Column("email_group_invitation")
    @Builder.Default
    private boolean emailGroupInvitation = true;

    @Column("email_reminders")
    @Builder.Default
    private boolean emailReminders = true;

    @Column("email_weekly_summary")
    @Builder.Default
    private boolean emailWeeklySummary = true;

    // Push preferences
    @Column("push_enabled")
    @Builder.Default
    private boolean pushEnabled = true;

    @Column("push_expense_added")
    @Builder.Default
    private boolean pushExpenseAdded = true;

    @Column("push_settlement_requested")
    @Builder.Default
    private boolean pushSettlementRequested = true;

    @Column("push_settlement_confirmed")
    @Builder.Default
    private boolean pushSettlementConfirmed = true;

    // In-app preferences
    @Column("in_app_enabled")
    @Builder.Default
    private boolean inAppEnabled = true;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;
}
