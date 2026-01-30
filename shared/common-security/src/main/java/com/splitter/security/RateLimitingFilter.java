package com.splitter.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter that limits requests per IP address.
 * Uses Token Bucket algorithm for smooth rate limiting.
 */
@Component
public class RateLimitingFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    // Rate limit configuration
    private static final int REQUESTS_PER_MINUTE = 60;
    private static final int AUTH_REQUESTS_PER_MINUTE = 10;
    private static final int BURST_CAPACITY = 10;

    // Bucket cache per IP
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();

    // Paths that need stricter rate limiting
    private static final String[] AUTH_PATHS = {
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/forgot-password",
        "/api/v1/auth/reset-password"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String clientIp = getClientIp(exchange);
        String path = exchange.getRequest().getPath().value();

        // Skip rate limiting for health checks
        if (path.startsWith("/actuator/health")) {
            return chain.filter(exchange);
        }

        // Determine which bucket to use
        Bucket bucket;
        if (isAuthPath(path)) {
            bucket = authBuckets.computeIfAbsent(clientIp, this::createAuthBucket);
        } else {
            bucket = buckets.computeIfAbsent(clientIp, this::createBucket);
        }

        // Try to consume a token
        if (bucket.tryConsume(1)) {
            // Add rate limit headers
            long availableTokens = bucket.getAvailableTokens();
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(availableTokens));
            exchange.getResponse().getHeaders().add("X-RateLimit-Limit", 
                String.valueOf(isAuthPath(path) ? AUTH_REQUESTS_PER_MINUTE : REQUESTS_PER_MINUTE));
            
            return chain.filter(exchange);
        } else {
            // Rate limit exceeded
            log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
            
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");
            exchange.getResponse().getHeaders().add("Retry-After", "60");
            
            return exchange.getResponse().setComplete();
        }
    }

    private Bucket createBucket(String key) {
        Bandwidth limit = Bandwidth.classic(REQUESTS_PER_MINUTE, 
            Refill.greedy(REQUESTS_PER_MINUTE, Duration.ofMinutes(1)));
        Bandwidth burst = Bandwidth.classic(BURST_CAPACITY, 
            Refill.intervally(BURST_CAPACITY, Duration.ofSeconds(1)));
        
        return Bucket.builder()
            .addLimit(limit)
            .addLimit(burst)
            .build();
    }

    private Bucket createAuthBucket(String key) {
        // Stricter rate limiting for auth endpoints
        Bandwidth limit = Bandwidth.classic(AUTH_REQUESTS_PER_MINUTE, 
            Refill.greedy(AUTH_REQUESTS_PER_MINUTE, Duration.ofMinutes(1)));
        
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    private boolean isAuthPath(String path) {
        for (String authPath : AUTH_PATHS) {
            if (path.equals(authPath)) {
                return true;
            }
        }
        return false;
    }

    private String getClientIp(ServerWebExchange exchange) {
        // Check for forwarded headers (when behind proxy/load balancer)
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            // Take the first IP in the chain (original client)
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }

        // Fall back to remote address
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        return "unknown";
    }

    /**
     * Clean up old buckets periodically to prevent memory leaks.
     * In production, this should be scheduled.
     */
    public void cleanupOldBuckets() {
        // Simple cleanup - remove buckets with full capacity (not recently used)
        buckets.entrySet().removeIf(entry -> 
            entry.getValue().getAvailableTokens() >= REQUESTS_PER_MINUTE);
        authBuckets.entrySet().removeIf(entry -> 
            entry.getValue().getAvailableTokens() >= AUTH_REQUESTS_PER_MINUTE);
    }
}
