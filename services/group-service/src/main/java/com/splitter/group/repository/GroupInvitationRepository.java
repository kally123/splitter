package com.splitter.group.repository;

import com.splitter.group.model.GroupInvitation;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for GroupInvitation entities.
 */
@Repository
public interface GroupInvitationRepository extends R2dbcRepository<GroupInvitation, UUID> {

    /**
     * Find invitation by token.
     */
    Mono<GroupInvitation> findByToken(String token);

    /**
     * Find pending invitations for a group.
     */
    Flux<GroupInvitation> findByGroupIdAndStatus(UUID groupId, GroupInvitation.InvitationStatus status);

    /**
     * Find pending invitations for an email.
     */
    Flux<GroupInvitation> findByInviteeEmailAndStatus(String email, GroupInvitation.InvitationStatus status);

    /**
     * Find pending invitations for a user.
     */
    Flux<GroupInvitation> findByInviteeUserIdAndStatus(UUID userId, GroupInvitation.InvitationStatus status);

    /**
     * Check if there's already a pending invitation.
     */
    Mono<Boolean> existsByGroupIdAndInviteeEmailAndStatus(
            UUID groupId, String email, GroupInvitation.InvitationStatus status);

    /**
     * Expire old invitations.
     */
    @Modifying
    @Query("UPDATE group_invitations SET status = 'EXPIRED' WHERE status = 'PENDING' AND expires_at < NOW()")
    Mono<Integer> expireOldInvitations();

    /**
     * Cancel all pending invitations for a group.
     */
    @Modifying
    @Query("UPDATE group_invitations SET status = 'CANCELLED' WHERE group_id = :groupId AND status = 'PENDING'")
    Mono<Integer> cancelAllPendingInvitations(UUID groupId);
}
