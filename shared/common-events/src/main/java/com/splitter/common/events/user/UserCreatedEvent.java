package com.splitter.common.events.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.splitter.common.events.BaseEvent;
import com.splitter.common.events.EventMetadata;
import com.splitter.common.events.EventTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a new user is created.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserCreatedEvent implements BaseEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private String eventType = EventTypes.USER_CREATED;

    @Builder.Default
    private Instant eventTime = Instant.now();

    @Builder.Default
    private String source = "user-service";

    private String subject; // user ID

    @Builder.Default
    private String dataVersion = "1.0";

    private UserData data;
    private EventMetadata metadata;

    /**
     * User data payload.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserData {
        private UUID userId;
        private String email;
        private String displayName;
        private String defaultCurrency;
        private String locale;
        private Instant createdAt;
    }
}
