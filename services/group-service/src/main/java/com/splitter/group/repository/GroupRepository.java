package com.splitter.group.repository;

import com.splitter.group.model.Group;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for Group entities.
 */
@Repository
public interface GroupRepository extends R2dbcRepository<Group, UUID> {

    /**
     * Find all active groups.
     */
    Flux<Group> findByActiveTrue();

    /**
     * Find groups created by a specific user.
     */
    Flux<Group> findByCreatedByAndActiveTrue(UUID createdBy);

    /**
     * Find groups by type.
     */
    Flux<Group> findByTypeAndActiveTrue(Group.GroupType type);

    /**
     * Find groups by name pattern.
     */
    @Query("SELECT * FROM groups WHERE LOWER(name) LIKE LOWER(CONCAT('%', :name, '%')) AND is_active = true")
    Flux<Group> searchByName(String name);

    /**
     * Count active groups for a user (as creator).
     */
    Mono<Long> countByCreatedByAndActiveTrue(UUID createdBy);
}
