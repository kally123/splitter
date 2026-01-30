package com.splitter.gateway.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time notifications and updates.
 */
@Slf4j
@Component
public class SplitterWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper;
    private final SecretKey jwtSecretKey;
    private final WebSocketSessionManager sessionManager;

    public SplitterWebSocketHandler(
            ObjectMapper objectMapper,
            @Value("${jwt.secret:your-256-bit-secret-key-here-change-in-production}") String jwtSecret,
            WebSocketSessionManager sessionManager) {
        this.objectMapper = objectMapper;
        this.jwtSecretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.sessionManager = sessionManager;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // Extract token from query parameter
        String query = session.getHandshakeInfo().getUri().getQuery();
        String token = extractTokenFromQuery(query);

        if (token == null) {
            log.warn("WebSocket connection rejected: No token provided");
            return session.close();
        }

        // Validate token and get user ID
        String userId;
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtSecretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            userId = claims.getSubject();
        } catch (Exception e) {
            log.warn("WebSocket connection rejected: Invalid token - {}", e.getMessage());
            return session.close();
        }

        log.info("WebSocket connected for user: {}", userId);

        // Register session
        sessionManager.addSession(userId, session);

        // Handle incoming messages and send outgoing messages
        Mono<Void> input = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(message -> handleIncomingMessage(userId, message))
                .doOnError(e -> log.error("WebSocket error for user {}: {}", userId, e.getMessage()))
                .doFinally(signalType -> {
                    log.info("WebSocket disconnected for user: {} ({})", userId, signalType);
                    sessionManager.removeSession(userId, session);
                })
                .then();

        // Send heartbeat every 30 seconds to keep connection alive
        Flux<WebSocketMessage> heartbeat = Flux.interval(Duration.ofSeconds(30))
                .map(tick -> session.textMessage("{\"type\":\"PING\"}"));

        // Get outgoing messages for this user
        Flux<WebSocketMessage> output = sessionManager.getMessageSink(userId)
                .asFlux()
                .map(session::textMessage)
                .mergeWith(heartbeat);

        Mono<Void> outputMono = session.send(output);

        return Mono.zip(input, outputMono).then();
    }

    private String extractTokenFromQuery(String query) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                return keyValue[1];
            }
        }
        return null;
    }

    private void handleIncomingMessage(String userId, String message) {
        log.debug("Received WebSocket message from user {}: {}", userId, message);
        
        try {
            Map<String, Object> messageMap = objectMapper.readValue(message, Map.class);
            String type = (String) messageMap.get("type");

            if ("PONG".equals(type)) {
                // Heartbeat response, ignore
                return;
            }

            // Handle other message types as needed
            log.debug("Handling message type {} from user {}", type, userId);
            
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse WebSocket message: {}", e.getMessage());
        }
    }
}
