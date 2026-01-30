package com.splitter.group.dto;

import com.splitter.group.model.Group;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Group data transfer object.
 */
@Builder
public record GroupDto(
    UUID id,
    String name,
    String description,
    Group.GroupType type,
    String defaultCurrency,
    String coverImageUrl,
    UUID createdBy,
    boolean simplifyDebts,
    int memberCount,
    Instant createdAt,
    Instant updatedAt
) {}
