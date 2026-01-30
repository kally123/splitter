package com.splitter.group.controller;

import com.splitter.group.dto.*;
import com.splitter.group.model.GroupMember;
import com.splitter.group.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST controller for group operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
@Tag(name = "Groups", description = "Group management operations")
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new group")
    public Mono<GroupDto> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("Creating group '{}' by user {}", request.name(), userId);
        return groupService.createGroup(request, userId);
    }

    @GetMapping
    @Operation(summary = "Get all groups for the current user")
    public Flux<GroupDto> getUserGroups(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.debug("Getting groups for user {}", userId);
        return groupService.getUserGroups(userId);
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "Get group by ID")
    public Mono<GroupDto> getGroup(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return groupService.getGroupById(groupId, userId);
    }

    @PutMapping("/{groupId}")
    @Operation(summary = "Update a group")
    public Mono<GroupDto> updateGroup(
            @PathVariable UUID groupId,
            @Valid @RequestBody UpdateGroupRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("Updating group {} by user {}", groupId, userId);
        return groupService.updateGroup(groupId, request, userId);
    }

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a group")
    public Mono<Void> deleteGroup(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("Deleting group {} by user {}", groupId, userId);
        return groupService.deleteGroup(groupId, userId);
    }

    @GetMapping("/{groupId}/members")
    @Operation(summary = "Get group members")
    public Flux<GroupMemberDto> getGroupMembers(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return groupService.getGroupMembers(groupId, userId);
    }

    @PostMapping("/{groupId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a member to a group")
    public Mono<GroupMemberDto> addMember(
            @PathVariable UUID groupId,
            @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID requesterId = UUID.fromString(jwt.getSubject());
        log.info("Adding member {} to group {} by user {}", request.userId(), groupId, requesterId);
        return groupService.addMember(groupId, request.userId(), request.displayName(), requesterId);
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a member from a group")
    public Mono<Void> removeMember(
            @PathVariable UUID groupId,
            @PathVariable UUID memberId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID requesterId = UUID.fromString(jwt.getSubject());
        log.info("Removing member {} from group {} by user {}", memberId, groupId, requesterId);
        return groupService.removeMember(groupId, memberId, requesterId);
    }

    @PostMapping("/{groupId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Leave a group")
    public Mono<Void> leaveGroup(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} leaving group {}", userId, groupId);
        return groupService.leaveGroup(groupId, userId);
    }

    @PutMapping("/{groupId}/members/{memberId}/role")
    @Operation(summary = "Update member role")
    public Mono<GroupMemberDto> updateMemberRole(
            @PathVariable UUID groupId,
            @PathVariable UUID memberId,
            @RequestBody UpdateRoleRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID requesterId = UUID.fromString(jwt.getSubject());
        log.info("Updating role of member {} in group {} to {}", memberId, groupId, request.role());
        return groupService.updateMemberRole(groupId, memberId, request.role(), requesterId);
    }

    /**
     * Request for adding a member.
     */
    public record AddMemberRequest(UUID userId, String displayName) {}

    /**
     * Request for updating member role.
     */
    public record UpdateRoleRequest(GroupMember.MemberRole role) {}
}
