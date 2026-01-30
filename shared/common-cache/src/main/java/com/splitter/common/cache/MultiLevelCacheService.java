package com.splitter.common.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Multi-level cache service combining local (L1) and Redis (L2) caches.
 * Provides optimal performance with local cache for hot data
 * and Redis for distributed consistency.
 * 
 * Cache hierarchy:
 * 1. L1 (Local) - In-memory, sub-millisecond access, per-instance
 * 2. L2 (Redis) - Distributed, millisecond access, shared across instances
 */
@Service
public class MultiLevelCacheService {

    private static final Logger log = LoggerFactory.getLogger(MultiLevelCacheService.class);
    
    private final LocalCacheService localCache;
    private final CacheInvalidationService redisCache;

    // Default TTLs
    private static final Duration DEFAULT_L1_TTL = Duration.ofMinutes(5);
    private static final Duration DEFAULT_L2_TTL = Duration.ofMinutes(30);

    public MultiLevelCacheService(LocalCacheService localCache, CacheInvalidationService redisCache) {
        this.localCache = localCache;
        this.redisCache = redisCache;
    }

    /**
     * Get a value from multi-level cache with default TTLs.
     * First checks L1 (local), then L2 (Redis), then computes if not found.
     */
    public <T> T get(String key, Class<T> type, Supplier<T> supplier) {
        return get(key, type, DEFAULT_L1_TTL, DEFAULT_L2_TTL, supplier);
    }

    /**
     * Get a value from multi-level cache with custom TTLs.
     */
    public <T> T get(String key, Class<T> type, Duration l1Ttl, Duration l2Ttl, Supplier<T> supplier) {
        // Try L1 (local cache) first
        T value = localCache.get(key, type);
        if (value != null) {
            log.trace("L1 cache hit: {}", key);
            return value;
        }

        // Try L2 (Redis) next
        value = redisCache.get(key, type);
        if (value != null) {
            log.trace("L2 cache hit: {}", key);
            // Promote to L1
            localCache.put(key, value, l1Ttl);
            return value;
        }

        // Cache miss - compute value
        log.trace("Cache miss: {}", key);
        value = supplier.get();
        if (value != null) {
            // Store in both levels
            localCache.put(key, value, l1Ttl);
            redisCache.setWithExpiry(key, value, l2Ttl);
        }

        return value;
    }

    /**
     * Invalidate a key from all cache levels.
     */
    public void evict(String key) {
        localCache.evict(key);
        redisCache.evict("cache", key);
        log.debug("Evicted from all cache levels: {}", key);
    }

    /**
     * Invalidate keys by prefix from all cache levels.
     */
    public void evictByPrefix(String prefix) {
        localCache.evictByPrefix(prefix);
        redisCache.evictByPattern(prefix + "*");
        log.debug("Evicted by prefix from all cache levels: {}", prefix);
    }

    /**
     * Set a value directly in both cache levels.
     */
    public <T> void set(String key, T value) {
        set(key, value, DEFAULT_L1_TTL, DEFAULT_L2_TTL);
    }

    /**
     * Set a value with custom TTLs.
     */
    public <T> void set(String key, T value, Duration l1Ttl, Duration l2Ttl) {
        localCache.put(key, value, l1Ttl);
        redisCache.setWithExpiry(key, value, l2Ttl);
    }

    /**
     * Check if a key exists in any cache level.
     */
    public boolean exists(String key) {
        return localCache.contains(key) || redisCache.exists(key);
    }

    /**
     * Clear all local cache entries.
     * Redis cache remains intact for other instances.
     */
    public void clearLocal() {
        localCache.clear();
    }

    /**
     * Get local cache statistics.
     */
    public int getLocalCacheSize() {
        return localCache.size();
    }
}
