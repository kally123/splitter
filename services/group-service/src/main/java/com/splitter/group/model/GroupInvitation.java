package com.splitter.group.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * GroupInvitation entity for managing pending invitations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("group_invitations")
public class GroupInvitation {

    @Id
    private UUID id;

    @Column("group_id")
    private UUID groupId;

    @Column("inviter_id")
    private UUID inviterId;

    @Column("invitee_email")
    private String inviteeEmail;

    @Column("invitee_user_id")
    private UUID inviteeUserId;

    @Column("token")
    private String token;

    @Column("status")
    @Builder.Default
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column("expires_at")
    private Instant expiresAt;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @Column("responded_at")
    private Instant respondedAt;

    /**
     * Invitation statuses.
     */
    public enum InvitationStatus {
        PENDING,
        ACCEPTED,
        DECLINED,
        EXPIRED,
        CANCELLED
    }

    /**
     * Check if invitation is expired.
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if invitation can be accepted.
     */
    public boolean canBeAccepted() {
        return status == InvitationStatus.PENDING && !isExpired();
    }
}
