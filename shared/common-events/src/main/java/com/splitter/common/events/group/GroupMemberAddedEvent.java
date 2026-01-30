package com.splitter.common.events.group;

import lombok.*;

import java.util.UUID;

/**
 * Event published when a member is added to a group.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberAddedEvent {

    private UUID groupId;
    private UUID userId;
    private String role;
}
