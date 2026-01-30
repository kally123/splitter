package com.splitter.common.cache;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

/**
 * Aspect for automatic cache invalidation based on annotations.
 */
@Aspect
@Component
public class CacheInvalidationAspect {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationAspect.class);
    
    private final CacheInvalidationService cacheInvalidationService;

    public CacheInvalidationAspect(CacheInvalidationService cacheInvalidationService) {
        this.cacheInvalidationService = cacheInvalidationService;
    }

    @AfterReturning("@annotation(invalidateUserCache)")
    public void handleUserCacheInvalidation(JoinPoint joinPoint, InvalidateUserCache invalidateUserCache) {
        try {
            Object[] args = joinPoint.getArgs();
            int userIdIndex = invalidateUserCache.userIdArgIndex();
            
            if (userIdIndex >= 0 && userIdIndex < args.length && args[userIdIndex] instanceof Long userId) {
                cacheInvalidationService.evictUserCaches(userId);
            }
        } catch (Exception e) {
            log.error("Failed to invalidate user cache", e);
        }
    }

    @AfterReturning("@annotation(invalidateGroupCache)")
    public void handleGroupCacheInvalidation(JoinPoint joinPoint, InvalidateGroupCache invalidateGroupCache) {
        try {
            Object[] args = joinPoint.getArgs();
            int groupIdIndex = invalidateGroupCache.groupIdArgIndex();
            
            if (groupIdIndex >= 0 && groupIdIndex < args.length && args[groupIdIndex] instanceof Long groupId) {
                cacheInvalidationService.evictGroupCaches(groupId);
            }
        } catch (Exception e) {
            log.error("Failed to invalidate group cache", e);
        }
    }

    @AfterReturning("@annotation(invalidateCaches)")
    public void handleMultipleCacheInvalidation(JoinPoint joinPoint, InvalidateCaches invalidateCaches) {
        try {
            for (String cacheName : invalidateCaches.value()) {
                cacheInvalidationService.evictAll(cacheName);
            }
        } catch (Exception e) {
            log.error("Failed to invalidate caches: {}", Arrays.toString(invalidateCaches.value()), e);
        }
    }
}

/**
 * Annotation to trigger user-related cache invalidation.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface InvalidateUserCache {
    int userIdArgIndex() default 0;
}

/**
 * Annotation to trigger group-related cache invalidation.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface InvalidateGroupCache {
    int groupIdArgIndex() default 0;
}

/**
 * Annotation to invalidate specific caches.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface InvalidateCaches {
    String[] value();
}
