package com.splitter.common.events.settlement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.splitter.common.events.BaseEvent;
import com.splitter.common.events.EventMetadata;
import com.splitter.common.events.EventTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a settlement is recorded.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SettlementRecordedEvent implements BaseEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private String eventType = EventTypes.SETTLEMENT_RECORDED;

    @Builder.Default
    private Instant eventTime = Instant.now();

    @Builder.Default
    private String source = "settlement-service";

    private String subject; // settlement ID

    @Builder.Default
    private String dataVersion = "1.0";

    private SettlementData data;
    private EventMetadata metadata;

    /**
     * Settlement data payload.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettlementData {
        private UUID settlementId;
        private UUID fromUserId;
        private UUID toUserId;
        private UUID groupId;
        private BigDecimal amount;
        private String currency;
        private String paymentMethod;
        private Instant settledAt;
        private UUID createdBy;
    }
}
