package com.splitter.common.events;

import java.time.Instant;

/**
 * Base interface for all domain events.
 * Follows the CloudEvents specification structure.
 */
public interface BaseEvent {

    /**
     * Unique identifier for this event instance.
     */
    String getEventId();

    /**
     * Type of event (e.g., "expense.created.v1").
     */
    String getEventType();

    /**
     * Timestamp when the event occurred.
     */
    Instant getEventTime();

    /**
     * Source service that produced the event.
     */
    String getSource();

    /**
     * Subject/aggregate ID the event relates to.
     */
    String getSubject();

    /**
     * Version of the event data schema.
     */
    String getDataVersion();

    /**
     * Event metadata (correlation ID, causation ID, etc.).
     */
    EventMetadata getMetadata();
}
