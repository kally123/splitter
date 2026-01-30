package com.splitter.group.controller;

import com.splitter.group.dto.GroupInvitationDto;
import com.splitter.group.dto.InviteRequest;
import com.splitter.group.service.InvitationService;
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
 * REST controller for group invitation operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Invitations", description = "Group invitation operations")
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping("/groups/{groupId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an invitation to join a group")
    public Mono<GroupInvitationDto> createInvitation(
            @PathVariable UUID groupId,
            @Valid @RequestBody InviteRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID inviterId = UUID.fromString(jwt.getSubject());
        log.info("Creating invitation for {} to group {} by user {}", request.email(), groupId, inviterId);
        return invitationService.createInvitation(groupId, request, inviterId);
    }

    @GetMapping("/groups/{groupId}/invitations")
    @Operation(summary = "Get pending invitations for a group")
    public Flux<GroupInvitationDto> getGroupInvitations(@PathVariable UUID groupId) {
        return invitationService.getGroupPendingInvitations(groupId);
    }

    @GetMapping("/invitations/pending")
    @Operation(summary = "Get pending invitations for the current user")
    public Flux<GroupInvitationDto> getUserPendingInvitations(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        return invitationService.getUserPendingInvitations(email);
    }

    @GetMapping("/invitations/{token}")
    @Operation(summary = "Get invitation details by token")
    public Mono<GroupInvitationDto> getInvitation(@PathVariable String token) {
        return invitationService.getInvitationByToken(token);
    }

    @PostMapping("/invitations/{token}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Accept an invitation")
    public Mono<Void> acceptInvitation(
            @PathVariable String token,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} accepting invitation {}", userId, token);
        return invitationService.acceptInvitation(token, userId);
    }

    @PostMapping("/invitations/{token}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Decline an invitation")
    public Mono<Void> declineInvitation(
            @PathVariable String token,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} declining invitation {}", userId, token);
        return invitationService.declineInvitation(token, userId);
    }

    @DeleteMapping("/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel an invitation")
    public Mono<Void> cancelInvitation(
            @PathVariable UUID invitationId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID requesterId = UUID.fromString(jwt.getSubject());
        log.info("User {} cancelling invitation {}", requesterId, invitationId);
        return invitationService.cancelInvitation(invitationId, requesterId);
    }
}
