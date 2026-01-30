package com.splitter.common.security;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Utility class for accessing the current authenticated user in reactive context.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Utility class
    }

    /**
     * Gets the current authenticated user from the security context.
     */
    public static Mono<AuthenticatedUser> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth.getPrincipal() instanceof Jwt)
                .map(auth -> (Jwt) auth.getPrincipal())
                .map(SecurityUtils::fromJwt);
    }

    /**
     * Gets the current user's ID.
     */
    public static Mono<UUID> getCurrentUserId() {
        return getCurrentUser().map(AuthenticatedUser::getUserId);
    }

    /**
     * Checks if the current user has a specific role.
     */
    public static Mono<Boolean> hasRole(String role) {
        return getCurrentUser()
                .map(user -> user.hasRole(role))
                .defaultIfEmpty(false);
    }

    /**
     * Checks if the current user is a member of a specific group.
     */
    public static Mono<Boolean> isMemberOfGroup(UUID groupId) {
        return getCurrentUser()
                .map(user -> user.isMemberOfGroup(groupId))
                .defaultIfEmpty(false);
    }

    /**
     * Creates an AuthenticatedUser from a JWT.
     */
    @SuppressWarnings("unchecked")
    public static AuthenticatedUser fromJwt(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        List<String> groupStrings = jwt.getClaimAsStringList("groups");
        List<UUID> groups = groupStrings != null
                ? groupStrings.stream().map(UUID::fromString).toList()
                : List.of();

        return AuthenticatedUser.builder()
                .userId(UUID.fromString(jwt.getSubject()))
                .email(jwt.getClaimAsString("email"))
                .displayName(jwt.getClaimAsString("name"))
                .roles(roles != null ? roles : List.of())
                .groupIds(groups)
                .build();
    }
}
