package com.splitter.common.events.group;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.splitter.common.events.BaseEvent;
import com.splitter.common.events.EventMetadata;
import com.splitter.common.events.EventTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event published when a new group is created.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroupCreatedEvent implements BaseEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private String eventType = EventTypes.GROUP_CREATED;

    @Builder.Default
    private Instant eventTime = Instant.now();

    @Builder.Default
    private String source = "group-service";

    private String subject; // group ID

    @Builder.Default
    private String dataVersion = "1.0";

    private GroupData data;
    private EventMetadata metadata;

    /**
     * Group data payload.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupData {
        private UUID groupId;
        private String name;
        private String groupType;
        private String defaultCurrency;
        private boolean simplifyDebts;
        private UUID createdBy;
        private List<UUID> initialMemberIds;
        private Instant createdAt;
    }
}
