package com.splitter.common.events.settlement;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event published when a settlement is confirmed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementCreatedEvent {

    private UUID settlementId;
    private UUID groupId;
    private UUID fromUserId;
    private UUID toUserId;
    private BigDecimal amount;
    private String currency;
}
