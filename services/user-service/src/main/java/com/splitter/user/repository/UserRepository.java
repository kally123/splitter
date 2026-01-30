package com.splitter.user.repository;

import com.splitter.user.model.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.UUID;

/**
 * Reactive repository for User entities.
 */
@Repository
public interface UserRepository extends R2dbcRepository<User, UUID> {

    /**
     * Find user by email address.
     */
    Mono<User> findByEmail(String email);

    /**
     * Find user by phone number.
     */
    Mono<User> findByPhoneNumber(String phoneNumber);

    /**
     * Check if email exists.
     */
    Mono<Boolean> existsByEmail(String email);

    /**
     * Check if phone number exists.
     */
    Mono<Boolean> existsByPhoneNumber(String phoneNumber);

    /**
     * Find users by IDs.
     */
    Flux<User> findByIdIn(Collection<UUID> ids);

    /**
     * Search users by email or display name.
     */
    @Query("SELECT * FROM users WHERE " +
           "LOWER(email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(display_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "LIMIT :limit")
    Flux<User> searchUsers(String searchTerm, int limit);

    /**
     * Find active users by IDs.
     */
    @Query("SELECT * FROM users WHERE id IN (:ids) AND is_active = true")
    Flux<User> findActiveUsersByIds(Collection<UUID> ids);
}
