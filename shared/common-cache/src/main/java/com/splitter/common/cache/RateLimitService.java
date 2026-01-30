package com.splitter.common.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for implementing distributed rate limiting using Redis.
 * Uses sliding window algorithm for accurate rate limiting.
 */
@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);
    
    private final RedisTemplate<String, Object> redisTemplate;

    public RateLimitService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Check if a request should be rate limited.
     * Uses a sliding window counter algorithm.
     *
     * @param key        Unique identifier (e.g., "ratelimit:user:123:endpoint")
     * @param maxRequests Maximum number of requests allowed
     * @param windowSeconds Time window in seconds
     * @return true if request is allowed, false if rate limited
     */
    public boolean isAllowed(String key, int maxRequests, int windowSeconds) {
        String redisKey = "ratelimit:" + key;
        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000L);

        try {
            // Remove old entries outside the window
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);

            // Count current entries in window
            Long count = redisTemplate.opsForZSet().zCard(redisKey);
            if (count == null) {
                count = 0L;
            }

            if (count >= maxRequests) {
                log.debug("Rate limit exceeded for key: {} (count: {}/{})", key, count, maxRequests);
                return false;
            }

            // Add current request
            redisTemplate.opsForZSet().add(redisKey, String.valueOf(now), now);
            
            // Set TTL slightly longer than window to ensure cleanup
            redisTemplate.expire(redisKey, windowSeconds + 10, TimeUnit.SECONDS);

            return true;
        } catch (Exception e) {
            log.error("Rate limit check failed for key: {}", key, e);
            // Fail open - allow request if Redis is unavailable
            return true;
        }
    }

    /**
     * Get remaining requests in the current window.
     */
    public int getRemainingRequests(String key, int maxRequests, int windowSeconds) {
        String redisKey = "ratelimit:" + key;
        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000L);

        try {
            // Remove old entries
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);

            Long count = redisTemplate.opsForZSet().zCard(redisKey);
            if (count == null) {
                count = 0L;
            }

            return Math.max(0, maxRequests - count.intValue());
        } catch (Exception e) {
            log.error("Failed to get remaining requests for key: {}", key, e);
            return maxRequests;
        }
    }

    /**
     * Reset rate limit for a key.
     */
    public void reset(String key) {
        String redisKey = "ratelimit:" + key;
        redisTemplate.delete(redisKey);
        log.debug("Rate limit reset for key: {}", key);
    }

    /**
     * API rate limit check with user context.
     */
    public RateLimitResult checkApiLimit(Long userId, String endpoint, int maxRequests, int windowSeconds) {
        String key = userId + ":" + endpoint;
        boolean allowed = isAllowed(key, maxRequests, windowSeconds);
        int remaining = getRemainingRequests(key, maxRequests, windowSeconds);
        
        return new RateLimitResult(allowed, remaining, maxRequests, windowSeconds);
    }

    /**
     * Rate limit result with metadata for response headers.
     */
    public record RateLimitResult(
        boolean allowed,
        int remaining,
        int limit,
        int windowSeconds
    ) {
        public long getResetTime() {
            return System.currentTimeMillis() + (windowSeconds * 1000L);
        }
    }
}
