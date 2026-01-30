package com.splitter.group.dto;

import com.splitter.group.model.GroupInvitation;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Group invitation data transfer object.
 */
@Builder
public record GroupInvitationDto(
    UUID id,
    UUID groupId,
    UUID inviterId,
    String inviteeEmail,
    GroupInvitation.InvitationStatus status,
    String token,
    Instant expiresAt,
    Instant createdAt
) {}
