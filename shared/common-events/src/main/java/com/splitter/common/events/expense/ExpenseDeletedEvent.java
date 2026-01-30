package com.splitter.common.events.expense;

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
import java.util.List;
import java.util.UUID;

/**
 * Event published when an expense is deleted.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExpenseDeletedEvent implements BaseEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private String eventType = EventTypes.EXPENSE_DELETED;

    @Builder.Default
    private Instant eventTime = Instant.now();

    @Builder.Default
    private String source = "expense-service";

    private String subject; // expense ID

    @Builder.Default
    private String dataVersion = "1.0";

    private ExpenseDeletedData data;
    private EventMetadata metadata;

    /**
     * Deleted expense data payload.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseDeletedData {
        private UUID expenseId;
        private UUID groupId;
        private BigDecimal amount;
        private String currency;
        private UUID paidBy;
        private List<UUID> affectedUserIds;
        private UUID deletedBy;
        private Instant deletedAt;
    }
}
