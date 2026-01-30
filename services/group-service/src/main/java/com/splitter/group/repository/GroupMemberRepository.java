package com.splitter.group.repository;

import com.splitter.group.model.GroupMember;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for GroupMember entities.
 */
@Repository
public interface GroupMemberRepository extends R2dbcRepository<GroupMember, UUID> {

    /**
     * Find all active members of a group.
     */
    Flux<GroupMember> findByGroupIdAndActiveTrue(UUID groupId);

    /**
     * Find all groups a user is a member of.
     */
    Flux<GroupMember> findByUserIdAndActiveTrue(UUID userId);

    /**
     * Find a specific membership.
     */
    Mono<GroupMember> findByGroupIdAndUserIdAndActiveTrue(UUID groupId, UUID userId);

    /**
     * Check if user is a member of a group.
     */
    Mono<Boolean> existsByGroupIdAndUserIdAndActiveTrue(UUID groupId, UUID userId);

    /**
     * Count active members in a group.
     */
    Mono<Long> countByGroupIdAndActiveTrue(UUID groupId);

    /**
     * Find members by role.
     */
    Flux<GroupMember> findByGroupIdAndRoleAndActiveTrue(UUID groupId, GroupMember.MemberRole role);

    /**
     * Deactivate a membership.
     */
    @Modifying
    @Query("UPDATE group_members SET is_active = false, left_at = NOW() WHERE group_id = :groupId AND user_id = :userId")
    Mono<Integer> deactivateMembership(UUID groupId, UUID userId);

    /**
     * Get all group IDs for a user.
     */
    @Query("SELECT group_id FROM group_members WHERE user_id = :userId AND is_active = true")
    Flux<UUID> findGroupIdsByUserId(UUID userId);
}
