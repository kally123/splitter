package com.splitter.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Rate limiting configuration using Redis.
 */
@Configuration
public class RateLimitConfig {

    /**
     * Key resolver based on user ID from JWT token.
     * Falls back to IP address for unauthenticated requests.
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Try to get user ID from JWT token
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                // In production, extract user ID from token
                // For now, use the token hash as key
                return Mono.just(String.valueOf(authHeader.hashCode()));
            }

            // Fall back to IP address
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just(ip);
        };
    }

    /**
     * Rate limiter for standard API requests.
     * Allows 100 requests per second with burst of 200.
     */
    @Bean
    public RedisRateLimiter standardRateLimiter() {
        return new RedisRateLimiter(100, 200, 1);
    }

    /**
     * Rate limiter for authentication endpoints.
     * More restrictive to prevent brute force attacks.
     * Allows 10 requests per second with burst of 20.
     */
    @Bean
    public RedisRateLimiter authRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }
}
