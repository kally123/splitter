package com.splitter.notification.service;

import com.splitter.common.events.EventTopics;
import com.splitter.common.events.expense.ExpenseCreatedEvent;
import com.splitter.common.events.group.GroupMemberAddedEvent;
import com.splitter.common.events.settlement.SettlementCreatedEvent;
import com.splitter.notification.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for consuming events and creating notifications.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventConsumerService {

    private final NotificationService notificationService;

    /**
     * Handle expense events.
     */
    @KafkaListener(topics = EventTopics.EXPENSE_EVENTS, groupId = "notification-service")
    public void handleExpenseEvent(Object event) {
        if (event instanceof ExpenseCreatedEvent expenseEvent) {
            handleExpenseCreated(expenseEvent);
        }
    }

    /**
     * Handle settlement events.
     */
    @KafkaListener(topics = EventTopics.SETTLEMENT_EVENTS, groupId = "notification-service")
    public void handleSettlementEvent(Object event) {
        if (event instanceof SettlementCreatedEvent settlementEvent) {
            handleSettlementCreated(settlementEvent);
        }
    }

    /**
     * Handle group events.
     */
    @KafkaListener(topics = EventTopics.GROUP_EVENTS, groupId = "notification-service")
    public void handleGroupEvent(Object event) {
        if (event instanceof GroupMemberAddedEvent memberEvent) {
            handleGroupMemberAdded(memberEvent);
        }
    }

    private void handleExpenseCreated(ExpenseCreatedEvent event) {
        log.info("Processing expense created event: {}", event.getExpenseId());

        // Notify all participants except the payer
        event.getShares().stream()
                .filter(share -> !share.getUserId().equals(event.getPaidBy()))
                .forEach(share -> {
                    notificationService.createNotification(
                            share.getUserId(),
                            Notification.NotificationType.EXPENSE_ADDED,
                            Notification.NotificationChannel.IN_APP,
                            "New expense: " + event.getDescription(),
                            String.format("You owe %s %s", event.getCurrency(), share.getAmount()),
                            Map.of(
                                    "expenseId", event.getExpenseId(),
                                    "groupId", event.getGroupId(),
                                    "amount", share.getAmount()
                            ),
                            "expense",
                            event.getExpenseId()
                    ).subscribe();
                });
    }

    private void handleSettlementCreated(SettlementCreatedEvent event) {
        log.info("Processing settlement created event: {}", event.getSettlementId());

        // Notify the recipient
        notificationService.createNotification(
                event.getToUserId(),
                Notification.NotificationType.SETTLEMENT_REQUESTED,
                Notification.NotificationChannel.IN_APP,
                "Settlement request received",
                String.format("You received a settlement of %s %s", 
                        event.getCurrency(), event.getAmount()),
                Map.of(
                        "settlementId", event.getSettlementId(),
                        "groupId", event.getGroupId(),
                        "amount", event.getAmount()
                ),
                "settlement",
                event.getSettlementId()
        ).subscribe();
    }

    private void handleGroupMemberAdded(GroupMemberAddedEvent event) {
        log.info("Processing group member added event: {} joined {}", 
                event.getUserId(), event.getGroupId());

        notificationService.createNotification(
                event.getUserId(),
                Notification.NotificationType.GROUP_MEMBER_JOINED,
                Notification.NotificationChannel.IN_APP,
                "Welcome to the group!",
                "You've been added to a new group",
                Map.of(
                        "groupId", event.getGroupId()
                ),
                "group",
                event.getGroupId()
        ).subscribe();
    }
}
