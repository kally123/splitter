package com.splitter.gateway.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka listener that broadcasts events to connected WebSocket clients.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "notification-events",
            groupId = "gateway-websocket-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleNotificationEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String eventType = event.path("eventType").asText();
            String userId = event.path("data").path("userId").asText();

            if (userId != null && !userId.isEmpty() && sessionManager.isUserConnected(userId)) {
                WebSocketSessionManager.WebSocketMessage wsMessage = 
                        WebSocketSessionManager.WebSocketMessage.of("NOTIFICATION", event);
                sessionManager.sendToUser(userId, wsMessage);
                log.debug("Sent notification event to user {}: {}", userId, eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process notification event: {}", e.getMessage());
        }
    }

    @KafkaListener(
            topics = "expense-events",
            groupId = "gateway-websocket-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleExpenseEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String eventType = event.path("eventType").asText();
            JsonNode affectedUsers = event.path("data").path("affectedUserIds");

            if (affectedUsers.isArray()) {
                for (JsonNode userIdNode : affectedUsers) {
                    String userId = userIdNode.asText();
                    if (sessionManager.isUserConnected(userId)) {
                        String wsEventType = switch (eventType) {
                            case "expense.created.v1" -> "EXPENSE_CREATED";
                            case "expense.updated.v1" -> "EXPENSE_UPDATED";
                            case "expense.deleted.v1" -> "EXPENSE_DELETED";
                            default -> "EXPENSE_UPDATE";
                        };
                        WebSocketSessionManager.WebSocketMessage wsMessage = 
                                WebSocketSessionManager.WebSocketMessage.of(wsEventType, event);
                        sessionManager.sendToUser(userId, wsMessage);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to process expense event: {}", e.getMessage());
        }
    }

    @KafkaListener(
            topics = "balance-events",
            groupId = "gateway-websocket-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleBalanceEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            JsonNode affectedUsers = event.path("data").path("affectedUserIds");

            if (affectedUsers.isArray()) {
                for (JsonNode userIdNode : affectedUsers) {
                    String userId = userIdNode.asText();
                    if (sessionManager.isUserConnected(userId)) {
                        WebSocketSessionManager.WebSocketMessage wsMessage = 
                                WebSocketSessionManager.WebSocketMessage.of("BALANCE_UPDATE", event);
                        sessionManager.sendToUser(userId, wsMessage);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to process balance event: {}", e.getMessage());
        }
    }

    @KafkaListener(
            topics = "settlement-events",
            groupId = "gateway-websocket-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleSettlementEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String fromUserId = event.path("data").path("fromUserId").asText();
            String toUserId = event.path("data").path("toUserId").asText();

            WebSocketSessionManager.WebSocketMessage wsMessage = 
                    WebSocketSessionManager.WebSocketMessage.of("SETTLEMENT_UPDATE", event);

            if (fromUserId != null && sessionManager.isUserConnected(fromUserId)) {
                sessionManager.sendToUser(fromUserId, wsMessage);
            }
            if (toUserId != null && sessionManager.isUserConnected(toUserId)) {
                sessionManager.sendToUser(toUserId, wsMessage);
            }
        } catch (Exception e) {
            log.error("Failed to process settlement event: {}", e.getMessage());
        }
    }

    @KafkaListener(
            topics = "group-events",
            groupId = "gateway-websocket-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleGroupEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            JsonNode memberIds = event.path("data").path("memberIds");

            if (memberIds.isArray()) {
                for (JsonNode userIdNode : memberIds) {
                    String userId = userIdNode.asText();
                    if (sessionManager.isUserConnected(userId)) {
                        WebSocketSessionManager.WebSocketMessage wsMessage = 
                                WebSocketSessionManager.WebSocketMessage.of("GROUP_UPDATE", event);
                        sessionManager.sendToUser(userId, wsMessage);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to process group event: {}", e.getMessage());
        }
    }
}
