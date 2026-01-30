package com.splitter.group.dto;

import com.splitter.group.model.GroupMember;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Group member data transfer object.
 */
@Builder
public record GroupMemberDto(
    UUID id,
    UUID groupId,
    UUID userId,
    String displayName,
    GroupMember.MemberRole role,
    Instant joinedAt
) {}
