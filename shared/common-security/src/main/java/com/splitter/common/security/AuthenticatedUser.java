package com.splitter.common.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Represents the authenticated user extracted from JWT token.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticatedUser {

    /**
     * User's unique identifier.
     */
    private UUID userId;

    /**
     * User's email address.
     */
    private String email;

    /**
     * User's display name.
     */
    private String displayName;

    /**
     * User's roles.
     */
    private List<String> roles;

    /**
     * User's group memberships.
     */
    private List<UUID> groupIds;

    /**
     * Whether the user has a specific role.
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * Whether the user is a member of a specific group.
     */
    public boolean isMemberOfGroup(UUID groupId) {
        return groupIds != null && groupIds.contains(groupId);
    }

    /**
     * Whether the user is an admin.
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
}
