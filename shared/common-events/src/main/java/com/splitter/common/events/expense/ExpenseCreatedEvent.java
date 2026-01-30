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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Event published when a new expense is created.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExpenseCreatedEvent implements BaseEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private String eventType = EventTypes.EXPENSE_CREATED;

    @Builder.Default
    private Instant eventTime = Instant.now();

    @Builder.Default
    private String source = "expense-service";

    private String subject; // expense ID

    @Builder.Default
    private String dataVersion = "1.0";

    private ExpenseData data;
    private EventMetadata metadata;

    /**
     * Expense data payload.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseData {
        private UUID expenseId;
        private UUID groupId;
        private String description;
        private BigDecimal amount;
        private String currency;
        private UUID paidBy;
        private String splitType;
        private LocalDate expenseDate;
        private UUID categoryId;
        private List<ShareData> shares;
        private UUID createdBy;
        private Instant createdAt;
    }

    /**
     * Share data for each participant.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShareData {
        private UUID userId;
        private BigDecimal amount;
        private boolean isPayer;
    }

    /**
     * Creates an ExpenseCreatedEvent from expense data.
     */
    public static ExpenseCreatedEvent of(ExpenseData data, EventMetadata metadata) {
        return ExpenseCreatedEvent.builder()
                .subject(data.getExpenseId().toString())
                .data(data)
                .metadata(metadata)
                .build();
    }
}
