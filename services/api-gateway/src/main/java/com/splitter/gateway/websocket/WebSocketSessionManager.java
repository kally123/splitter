package com.splitter.gateway.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages WebSocket sessions for connected users.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionManager {

    private final ObjectMapper objectMapper;
    
    // Map of userId -> list of sessions (user can have multiple connections)
    private final Map<String, List<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    
    // Map of userId -> message sink for sending messages
    private final Map<String, Sinks.Many<String>> userMessageSinks = new ConcurrentHashMap<>();

    /**
     * Add a session for a user.
     */
    public void addSession(String userId, WebSocketSession session) {
        userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(session);
        userMessageSinks.computeIfAbsent(userId, k -> Sinks.many().multicast().onBackpressureBuffer());
        log.debug("Added WebSocket session for user {}. Total sessions: {}", 
                userId, userSessions.get(userId).size());
    }

    /**
     * Remove a session for a user.
     */
    public void removeSession(String userId, WebSocketSession session) {
        List<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
                Sinks.Many<String> sink = userMessageSinks.remove(userId);
                if (sink != null) {
                    sink.tryEmitComplete();
                }
            }
            log.debug("Removed WebSocket session for user {}. Remaining sessions: {}", 
                    userId, sessions.size());
        }
    }

    /**
     * Get the message sink for a user.
     */
    public Sinks.Many<String> getMessageSink(String userId) {
        return userMessageSinks.computeIfAbsent(userId, 
                k -> Sinks.many().multicast().onBackpressureBuffer());
    }

    /**
     * Send a message to a specific user.
     */
    public void sendToUser(String userId, WebSocketMessage message) {
        Sinks.Many<String> sink = userMessageSinks.get(userId);
        if (sink != null) {
            try {
                String json = objectMapper.writeValueAsString(message);
                sink.tryEmitNext(json);
                log.debug("Sent message to user {}: {}", userId, message.type());
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize WebSocket message: {}", e.getMessage());
            }
        }
    }

    /**
     * Send a message to multiple users.
     */
    public void sendToUsers(List<String> userIds, WebSocketMessage message) {
        for (String userId : userIds) {
            sendToUser(userId, message);
        }
    }

    /**
     * Broadcast a message to all connected users.
     */
    public void broadcast(WebSocketMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            userMessageSinks.values().forEach(sink -> sink.tryEmitNext(json));
            log.debug("Broadcast message to {} users: {}", userMessageSinks.size(), message.type());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize WebSocket broadcast message: {}", e.getMessage());
        }
    }

    /**
     * Check if a user is connected.
     */
    public boolean isUserConnected(String userId) {
        List<WebSocketSession> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * Get count of connected users.
     */
    public int getConnectedUserCount() {
        return userSessions.size();
    }

    /**
     * WebSocket message record.
     */
    public record WebSocketMessage(
            String type,
            Object data,
            String timestamp
    ) {
        public static WebSocketMessage of(String type, Object data) {
            return new WebSocketMessage(type, data, java.time.Instant.now().toString());
        }
    }
}
