package com.splitter.notification.repository;

import com.splitter.notification.model.NotificationPreference;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for NotificationPreference entities.
 */
@Repository
public interface NotificationPreferenceRepository extends R2dbcRepository<NotificationPreference, UUID> {

    /**
     * Find preferences by user ID.
     */
    Mono<NotificationPreference> findByUserId(UUID userId);
}
