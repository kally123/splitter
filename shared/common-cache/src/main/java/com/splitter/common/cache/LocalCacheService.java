package com.splitter.common.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Local in-memory cache for frequently accessed, small data.
 * Acts as L1 cache in front of Redis (L2).
 * Provides sub-millisecond access for hot data.
 */
@Service
public class LocalCacheService {

    private static final Logger log = LoggerFactory.getLogger(LocalCacheService.class);
    
    private final Map<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    public LocalCacheService() {
        // Schedule periodic cleanup of expired entries
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpired, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Put a value in the local cache with TTL.
     */
    public <T> void put(String key, T value, Duration ttl) {
        long expiresAt = System.currentTimeMillis() + ttl.toMillis();
        cache.put(key, new CacheEntry<>(value, expiresAt));
        log.trace("Cached key locally: {} (expires in {} seconds)", key, ttl.getSeconds());
    }

    /**
     * Get a value from the local cache.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        CacheEntry<?> entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        
        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        
        Object value = entry.getValue();
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Get a value or compute it if not present.
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrCompute(String key, Class<T> type, Duration ttl, java.util.function.Supplier<T> supplier) {
        T cached = get(key, type);
        if (cached != null) {
            return cached;
        }
        
        T value = supplier.get();
        if (value != null) {
            put(key, value, ttl);
        }
        return value;
    }

    /**
     * Remove a specific key from the cache.
     */
    public void evict(String key) {
        cache.remove(key);
    }

    /**
     * Remove all keys matching a prefix.
     */
    public void evictByPrefix(String prefix) {
        cache.keySet().removeIf(key -> key.startsWith(prefix));
    }

    /**
     * Clear the entire cache.
     */
    public void clear() {
        cache.clear();
        log.info("Local cache cleared");
    }

    /**
     * Get the current size of the cache.
     */
    public int size() {
        return cache.size();
    }

    /**
     * Check if a key exists and is not expired.
     */
    public boolean contains(String key) {
        CacheEntry<?> entry = cache.get(key);
        if (entry == null) {
            return false;
        }
        if (entry.isExpired()) {
            cache.remove(key);
            return false;
        }
        return true;
    }

    private void cleanupExpired() {
        int before = cache.size();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int removed = before - cache.size();
        if (removed > 0) {
            log.debug("Cleaned up {} expired local cache entries", removed);
        }
    }

    private static class CacheEntry<T> {
        private final T value;
        private final long expiresAt;

        CacheEntry(T value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }

        T getValue() {
            return value;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
