package com.splitter.common.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata for event tracing and correlation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventMetadata {

    /**
     * Correlation ID for tracing requests across services.
     */
    private String correlationId;

    /**
     * ID of the command/event that caused this event.
     */
    private String causationId;

    /**
     * User ID that triggered the action.
     */
    private String userId;

    /**
     * Tenant ID for multi-tenant scenarios.
     */
    private String tenantId;

    /**
     * IP address of the request origin.
     */
    private String sourceIp;

    /**
     * User agent of the request origin.
     */
    private String userAgent;

    /**
     * Creates metadata with correlation ID.
     */
    public static EventMetadata withCorrelationId(String correlationId) {
        return EventMetadata.builder()
                .correlationId(correlationId)
                .build();
    }

    /**
     * Creates metadata for a user action.
     */
    public static EventMetadata forUser(String userId, String correlationId) {
        return EventMetadata.builder()
                .userId(userId)
                .correlationId(correlationId)
                .build();
    }
}
