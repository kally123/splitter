package com.splitter.common.events;

/**
 * Constants for event type names.
 * Format: {domain}.{action}.{version}
 */
public final class EventTypes {

    private EventTypes() {
        // Utility class
    }

    // User events
    public static final String USER_CREATED = "user.created.v1";
    public static final String USER_UPDATED = "user.updated.v1";
    public static final String USER_DELETED = "user.deleted.v1";
    public static final String FRIENDSHIP_CREATED = "friendship.created.v1";
    public static final String FRIENDSHIP_ACCEPTED = "friendship.accepted.v1";
    public static final String FRIENDSHIP_DELETED = "friendship.deleted.v1";

    // Group events
    public static final String GROUP_CREATED = "group.created.v1";
    public static final String GROUP_UPDATED = "group.updated.v1";
    public static final String GROUP_DELETED = "group.deleted.v1";
    public static final String MEMBER_ADDED = "group.member.added.v1";
    public static final String MEMBER_REMOVED = "group.member.removed.v1";
    public static final String MEMBER_ROLE_CHANGED = "group.member.role.changed.v1";

    // Expense events
    public static final String EXPENSE_CREATED = "expense.created.v1";
    public static final String EXPENSE_UPDATED = "expense.updated.v1";
    public static final String EXPENSE_DELETED = "expense.deleted.v1";

    // Balance events
    public static final String BALANCE_UPDATED = "balance.updated.v1";
    public static final String BALANCE_RECALCULATED = "balance.recalculated.v1";

    // Settlement events
    public static final String SETTLEMENT_RECORDED = "settlement.recorded.v1";
    public static final String SETTLEMENT_DELETED = "settlement.deleted.v1";

    // Notification events
    public static final String NOTIFICATION_SENT = "notification.sent.v1";
    public static final String NOTIFICATION_READ = "notification.read.v1";
}
