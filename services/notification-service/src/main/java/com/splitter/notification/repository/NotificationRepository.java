package com.splitter.notification.repository;

import com.splitter.notification.model.Notification;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Reactive repository for Notification entities.
 */
@Repository
public interface NotificationRepository extends R2dbcRepository<Notification, UUID> {

    /**
     * Find notifications for a user.
     */
    Flux<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find notifications for a user with pagination.
     */
    @Query("SELECT * FROM notifications WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<Notification> findByUserId(UUID userId, int limit, int offset);

    /**
     * Find unread notifications for a user.
     */
    Flux<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(UUID userId);

    /**
     * Find pending notifications to send.
     */
    Flux<Notification> findByStatus(Notification.NotificationStatus status);

    /**
     * Find pending notifications by channel.
     */
    Flux<Notification> findByStatusAndChannel(
            Notification.NotificationStatus status, 
            Notification.NotificationChannel channel);

    /**
     * Count unread notifications for a user.
     */
    Mono<Long> countByUserIdAndReadFalse(UUID userId);

    /**
     * Mark notification as read.
     */
    @Modifying
    @Query("UPDATE notifications SET is_read = true, read_at = :readAt WHERE id = :notificationId")
    Mono<Integer> markAsRead(UUID notificationId, Instant readAt);

    /**
     * Mark all notifications as read for a user.
     */
    @Modifying
    @Query("UPDATE notifications SET is_read = true, read_at = :readAt WHERE user_id = :userId AND is_read = false")
    Mono<Integer> markAllAsRead(UUID userId, Instant readAt);

    /**
     * Delete old notifications.
     */
    @Modifying
    @Query("DELETE FROM notifications WHERE created_at < :olderThan AND is_read = true")
    Mono<Integer> deleteOldReadNotifications(Instant olderThan);
}
