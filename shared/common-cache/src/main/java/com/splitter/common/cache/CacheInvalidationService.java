package com.splitter.common.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Service for cache invalidation operations.
 * Provides pattern-based and targeted cache eviction.
 */
@Service
public class CacheInvalidationService {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationService.class);
    
    private final RedisTemplate<String, Object> redisTemplate;

    public CacheInvalidationService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Evict a specific cache entry.
     */
    public void evict(String cacheName, String key) {
        String fullKey = buildCacheKey(cacheName, key);
        Boolean deleted = redisTemplate.delete(fullKey);
        log.debug("Evicted cache key: {} - success: {}", fullKey, deleted);
    }

    /**
     * Evict all entries in a cache.
     */
    public void evictAll(String cacheName) {
        String pattern = cacheName + "::*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            Long deleted = redisTemplate.delete(keys);
            log.info("Evicted {} keys from cache: {}", deleted, cacheName);
        }
    }

    /**
     * Evict cache entries matching a pattern.
     */
    public void evictByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            Long deleted = redisTemplate.delete(keys);
            log.info("Evicted {} keys matching pattern: {}", deleted, pattern);
        }
    }

    /**
     * Evict all caches related to a user.
     */
    public void evictUserCaches(Long userId) {
        evictByPattern("*::" + userId + ":*");
        evictByPattern("*::user:" + userId + ":*");
        evictByPattern("*::userId=" + userId + "*");
        log.info("Evicted all caches for user: {}", userId);
    }

    /**
     * Evict all caches related to a group.
     */
    public void evictGroupCaches(Long groupId) {
        evict(CacheNames.GROUP_DETAILS, String.valueOf(groupId));
        evict(CacheNames.GROUP_SUMMARY, String.valueOf(groupId));
        evict(CacheNames.GROUP_MEMBERS, String.valueOf(groupId));
        evict(CacheNames.GROUP_BALANCES, String.valueOf(groupId));
        evictByPattern("*::group:" + groupId + ":*");
        log.info("Evicted all caches for group: {}", groupId);
    }

    /**
     * Evict caches after an expense is modified.
     */
    public void evictExpenseCaches(Long expenseId, Long groupId, Collection<Long> participantIds) {
        evict(CacheNames.EXPENSE_DETAILS, String.valueOf(expenseId));
        evict(CacheNames.GROUP_BALANCES, String.valueOf(groupId));
        evictByPattern(CacheNames.EXPENSE_LIST + "::group:" + groupId + ":*");
        
        // Evict balance caches for all participants
        for (Long userId : participantIds) {
            evict(CacheNames.USER_BALANCES, String.valueOf(userId));
            evictByPattern(CacheNames.ANALYTICS_SUMMARY + "::user:" + userId + ":*");
        }
        
        log.info("Evicted expense caches for expense: {}, group: {}", expenseId, groupId);
    }

    /**
     * Set a value with expiration.
     */
    public void setWithExpiry(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Get a cached value.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Check if a key exists in cache.
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Extend the TTL of a cache key.
     */
    public boolean extendTtl(String key, Duration ttl) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, ttl.toMillis(), TimeUnit.MILLISECONDS));
    }

    private String buildCacheKey(String cacheName, String key) {
        return cacheName + "::" + key;
    }
}
