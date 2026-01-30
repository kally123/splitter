package com.splitter.common.events;

/**
 * Constants for Kafka topic names.
 */
public final class EventTopics {

    private EventTopics() {
        // Utility class
    }

    // User domain events
    public static final String USER_EVENTS = "user.events";

    // Group domain events
    public static final String GROUP_EVENTS = "group.events";

    // Expense domain events
    public static final String EXPENSE_EVENTS = "expense.events";

    // Balance domain events
    public static final String BALANCE_EVENTS = "balance.events";

    // Settlement domain events
    public static final String SETTLEMENT_EVENTS = "settlement.events";

    // Notification domain events
    public static final String NOTIFICATION_EVENTS = "notification.events";

    // Dead letter queue
    public static final String DLQ_EVENTS = "dlq.events";
}
