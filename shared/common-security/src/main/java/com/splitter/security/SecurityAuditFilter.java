package com.splitter.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Security audit logging filter.
 * Logs security-relevant events for compliance and incident response.
 */
@Component
public class SecurityAuditFilter implements WebFilter {

    private static final Logger auditLog = LoggerFactory.getLogger("SECURITY_AUDIT");
    private static final Logger log = LoggerFactory.getLogger(SecurityAuditFilter.class);

    // Paths that should be audited
    private static final String[] AUDITED_PATHS = {
        "/api/v1/auth",
        "/api/v1/admin",
        "/api/v1/users/me",
        "/api/v1/users/*/password"
    };

    // Sensitive paths that need extra logging
    private static final String[] SENSITIVE_PATHS = {
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/forgot-password",
        "/api/v1/admin"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        
        // Skip non-audited paths
        if (!shouldAudit(path)) {
            return chain.filter(exchange);
        }

        String requestId = getOrCreateRequestId(exchange);
        String clientIp = getClientIp(exchange);
        String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
        Instant startTime = Instant.now();

        // Log incoming request
        logSecurityEvent(SecurityEvent.builder()
            .type(SecurityEventType.REQUEST_RECEIVED)
            .requestId(requestId)
            .clientIp(clientIp)
            .method(method)
            .path(path)
            .userAgent(userAgent)
            .timestamp(startTime)
            .build());

        return chain.filter(exchange)
            .doOnSuccess(v -> {
                int statusCode = exchange.getResponse().getStatusCode() != null 
                    ? exchange.getResponse().getStatusCode().value() 
                    : 0;
                
                SecurityEventType eventType = determineEventType(path, method, statusCode);
                
                logSecurityEvent(SecurityEvent.builder()
                    .type(eventType)
                    .requestId(requestId)
                    .clientIp(clientIp)
                    .method(method)
                    .path(path)
                    .statusCode(statusCode)
                    .durationMs(Instant.now().toEpochMilli() - startTime.toEpochMilli())
                    .timestamp(Instant.now())
                    .build());
            })
            .doOnError(error -> {
                logSecurityEvent(SecurityEvent.builder()
                    .type(SecurityEventType.REQUEST_ERROR)
                    .requestId(requestId)
                    .clientIp(clientIp)
                    .method(method)
                    .path(path)
                    .error(error.getMessage())
                    .timestamp(Instant.now())
                    .build());
            });
    }

    private boolean shouldAudit(String path) {
        for (String auditedPath : AUDITED_PATHS) {
            if (path.startsWith(auditedPath) || matchesPattern(path, auditedPath)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesPattern(String path, String pattern) {
        // Simple wildcard matching
        if (pattern.contains("*")) {
            String regex = pattern.replace("*", ".*");
            return path.matches(regex);
        }
        return path.startsWith(pattern);
    }

    private SecurityEventType determineEventType(String path, String method, int statusCode) {
        if (path.contains("/login")) {
            return statusCode == 200 ? SecurityEventType.LOGIN_SUCCESS : SecurityEventType.LOGIN_FAILURE;
        }
        if (path.contains("/register")) {
            return statusCode == 201 ? SecurityEventType.REGISTRATION_SUCCESS : SecurityEventType.REGISTRATION_FAILURE;
        }
        if (path.contains("/password")) {
            return statusCode < 400 ? SecurityEventType.PASSWORD_CHANGE : SecurityEventType.PASSWORD_CHANGE_FAILURE;
        }
        if (path.contains("/admin")) {
            return SecurityEventType.ADMIN_ACTION;
        }
        if (statusCode == 401) {
            return SecurityEventType.UNAUTHORIZED_ACCESS;
        }
        if (statusCode == 403) {
            return SecurityEventType.FORBIDDEN_ACCESS;
        }
        return SecurityEventType.REQUEST_COMPLETED;
    }

    private void logSecurityEvent(SecurityEvent event) {
        String logMessage = String.format(
            "type=%s requestId=%s clientIp=%s method=%s path=%s statusCode=%d durationMs=%d",
            event.type,
            event.requestId,
            event.clientIp,
            event.method,
            event.path,
            event.statusCode,
            event.durationMs
        );

        switch (event.type) {
            case LOGIN_FAILURE:
            case UNAUTHORIZED_ACCESS:
            case FORBIDDEN_ACCESS:
            case PASSWORD_CHANGE_FAILURE:
                auditLog.warn(logMessage);
                break;
            case LOGIN_SUCCESS:
            case ADMIN_ACTION:
            case PASSWORD_CHANGE:
                auditLog.info(logMessage);
                break;
            default:
                if (log.isDebugEnabled()) {
                    auditLog.debug(logMessage);
                }
        }
    }

    private String getOrCreateRequestId(ServerWebExchange exchange) {
        String requestId = exchange.getRequest().getHeaders().getFirst("X-Request-ID");
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
            exchange.getResponse().getHeaders().add("X-Request-ID", requestId);
        }
        return requestId;
    }

    private String getClientIp(ServerWebExchange exchange) {
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }

    public enum SecurityEventType {
        REQUEST_RECEIVED,
        REQUEST_COMPLETED,
        REQUEST_ERROR,
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        LOGOUT,
        REGISTRATION_SUCCESS,
        REGISTRATION_FAILURE,
        PASSWORD_CHANGE,
        PASSWORD_CHANGE_FAILURE,
        UNAUTHORIZED_ACCESS,
        FORBIDDEN_ACCESS,
        ADMIN_ACTION,
        TOKEN_REFRESH,
        TOKEN_REVOCATION
    }

    public static class SecurityEvent {
        final SecurityEventType type;
        final String requestId;
        final String clientIp;
        final String method;
        final String path;
        final String userAgent;
        final String userId;
        final int statusCode;
        final long durationMs;
        final String error;
        final Instant timestamp;

        private SecurityEvent(Builder builder) {
            this.type = builder.type;
            this.requestId = builder.requestId;
            this.clientIp = builder.clientIp;
            this.method = builder.method;
            this.path = builder.path;
            this.userAgent = builder.userAgent;
            this.userId = builder.userId;
            this.statusCode = builder.statusCode;
            this.durationMs = builder.durationMs;
            this.error = builder.error;
            this.timestamp = builder.timestamp;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private SecurityEventType type;
            private String requestId;
            private String clientIp;
            private String method;
            private String path;
            private String userAgent;
            private String userId;
            private int statusCode;
            private long durationMs;
            private String error;
            private Instant timestamp;

            public Builder type(SecurityEventType type) { this.type = type; return this; }
            public Builder requestId(String requestId) { this.requestId = requestId; return this; }
            public Builder clientIp(String clientIp) { this.clientIp = clientIp; return this; }
            public Builder method(String method) { this.method = method; return this; }
            public Builder path(String path) { this.path = path; return this; }
            public Builder userAgent(String userAgent) { this.userAgent = userAgent; return this; }
            public Builder userId(String userId) { this.userId = userId; return this; }
            public Builder statusCode(int statusCode) { this.statusCode = statusCode; return this; }
            public Builder durationMs(long durationMs) { this.durationMs = durationMs; return this; }
            public Builder error(String error) { this.error = error; return this; }
            public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }

            public SecurityEvent build() {
                return new SecurityEvent(this);
            }
        }
    }
}
