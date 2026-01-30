package com.splitter.user.controller;

import com.splitter.common.dto.user.CreateUserRequest;
import com.splitter.common.dto.user.UserDto;
import com.splitter.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for user operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management operations")
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new user", description = "Register a new user account")
    public Mono<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("Creating new user with email: {}", request.email());
        return userService.createUser(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get the currently authenticated user's profile")
    public Mono<UserDto> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.debug("Getting current user: {}", userId);
        return userService.getUserById(userId);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Get a user's public profile by their ID")
    public Mono<UserDto> getUserById(@PathVariable UUID userId) {
        log.debug("Getting user by ID: {}", userId);
        return userService.getUserById(userId);
    }

    @GetMapping("/batch")
    @Operation(summary = "Get users by IDs", description = "Get multiple users by their IDs")
    public Flux<UserDto> getUsersByIds(@RequestParam List<UUID> ids) {
        log.debug("Getting users by IDs: {}", ids);
        return userService.getUsersByIds(ids);
    }

    @GetMapping("/search")
    @Operation(summary = "Search users", description = "Search for users by email or display name")
    public Flux<UserDto> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {
        log.debug("Searching users with query: {}", q);
        return userService.searchUsers(q, Math.min(limit, 50));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user", description = "Update the currently authenticated user's profile")
    public Mono<UserDto> updateCurrentUser(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserService.UpdateUserRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("Updating user: {}", userId);
        return userService.updateUser(userId, request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate current user", description = "Deactivate the currently authenticated user's account")
    public Mono<Void> deactivateCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("Deactivating user: {}", userId);
        return userService.deactivateUser(userId);
    }
}
