package com.splitter.group.service;

import com.splitter.group.dto.GroupInvitationDto;
import com.splitter.group.dto.InviteRequest;
import com.splitter.group.exception.GroupNotFoundException;
import com.splitter.group.exception.InvitationNotFoundException;
import com.splitter.group.model.Group;
import com.splitter.group.model.GroupInvitation;
import com.splitter.group.model.GroupMember;
import com.splitter.group.repository.GroupInvitationRepository;
import com.splitter.group.repository.GroupMemberRepository;
import com.splitter.group.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Service for managing group invitations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationService {

    private final GroupInvitationRepository invitationRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final GroupService groupService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.invitation.expiry-days:7}")
    private int invitationExpiryDays;

    /**
     * Create an invitation to join a group.
     */
    @Transactional
    public Mono<GroupInvitationDto> createInvitation(UUID groupId, InviteRequest request, UUID inviterId) {
        return groupRepository.findById(groupId)
                .filter(Group::isActive)
                .switchIfEmpty(Mono.error(new GroupNotFoundException(groupId)))
                .flatMap(group -> checkExistingInvitation(groupId, request.email()))
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalArgumentException("Invitation already pending for this email"));
                    }
                    
                    GroupInvitation invitation = GroupInvitation.builder()
                            .groupId(groupId)
                            .inviterId(inviterId)
                            .inviteeEmail(request.email().toLowerCase())
                            .token(UUID.randomUUID().toString())
                            .expiresAt(Instant.now().plus(invitationExpiryDays, ChronoUnit.DAYS))
                            .createdAt(Instant.now())
                            .build();
                    
                    return invitationRepository.save(invitation);
                })
                .doOnSuccess(this::sendInvitationEmail)
                .map(this::toDto);
    }

    /**
     * Accept an invitation.
     */
    @Transactional
    public Mono<Void> acceptInvitation(String token, UUID userId) {
        return invitationRepository.findByToken(token)
                .switchIfEmpty(Mono.error(new InvitationNotFoundException(token)))
                .flatMap(invitation -> {
                    if (!invitation.canBeAccepted()) {
                        return Mono.error(new IllegalArgumentException("Invitation is no longer valid"));
                    }
                    
                    invitation.setStatus(GroupInvitation.InvitationStatus.ACCEPTED);
                    invitation.setInviteeUserId(userId);
                    invitation.setRespondedAt(Instant.now());
                    
                    return invitationRepository.save(invitation)
                            .then(groupService.addMember(invitation.getGroupId(), userId, null, invitation.getInviterId()))
                            .then();
                });
    }

    /**
     * Decline an invitation.
     */
    @Transactional
    public Mono<Void> declineInvitation(String token, UUID userId) {
        return invitationRepository.findByToken(token)
                .switchIfEmpty(Mono.error(new InvitationNotFoundException(token)))
                .flatMap(invitation -> {
                    if (invitation.getStatus() != GroupInvitation.InvitationStatus.PENDING) {
                        return Mono.error(new IllegalArgumentException("Invitation is no longer pending"));
                    }
                    
                    invitation.setStatus(GroupInvitation.InvitationStatus.DECLINED);
                    invitation.setInviteeUserId(userId);
                    invitation.setRespondedAt(Instant.now());
                    
                    return invitationRepository.save(invitation).then();
                });
    }

    /**
     * Cancel an invitation.
     */
    @Transactional
    public Mono<Void> cancelInvitation(UUID invitationId, UUID requesterId) {
        return invitationRepository.findById(invitationId)
                .switchIfEmpty(Mono.error(new InvitationNotFoundException(invitationId.toString())))
                .flatMap(invitation -> {
                    if (invitation.getStatus() != GroupInvitation.InvitationStatus.PENDING) {
                        return Mono.error(new IllegalArgumentException("Only pending invitations can be cancelled"));
                    }
                    
                    invitation.setStatus(GroupInvitation.InvitationStatus.CANCELLED);
                    invitation.setRespondedAt(Instant.now());
                    
                    return invitationRepository.save(invitation).then();
                });
    }

    /**
     * Get pending invitations for a group.
     */
    public Flux<GroupInvitationDto> getGroupPendingInvitations(UUID groupId) {
        return invitationRepository.findByGroupIdAndStatus(groupId, GroupInvitation.InvitationStatus.PENDING)
                .map(this::toDto);
    }

    /**
     * Get pending invitations for a user (by email).
     */
    public Flux<GroupInvitationDto> getUserPendingInvitations(String email) {
        return invitationRepository.findByInviteeEmailAndStatus(email.toLowerCase(), GroupInvitation.InvitationStatus.PENDING)
                .filter(inv -> !inv.isExpired())
                .map(this::toDto);
    }

    /**
     * Get invitation by token.
     */
    public Mono<GroupInvitationDto> getInvitationByToken(String token) {
        return invitationRepository.findByToken(token)
                .switchIfEmpty(Mono.error(new InvitationNotFoundException(token)))
                .map(this::toDto);
    }

    /**
     * Expire old invitations (scheduled job).
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void expireOldInvitations() {
        invitationRepository.expireOldInvitations()
                .subscribe(count -> log.info("Expired {} old invitations", count));
    }

    // Private helper methods

    private Mono<Boolean> checkExistingInvitation(UUID groupId, String email) {
        return invitationRepository.existsByGroupIdAndInviteeEmailAndStatus(
                groupId, email.toLowerCase(), GroupInvitation.InvitationStatus.PENDING);
    }

    private void sendInvitationEmail(GroupInvitation invitation) {
        // TODO: Publish event to notification service
        log.info("Invitation created for email: {} to group: {}", 
                invitation.getInviteeEmail(), invitation.getGroupId());
    }

    private GroupInvitationDto toDto(GroupInvitation invitation) {
        return GroupInvitationDto.builder()
                .id(invitation.getId())
                .groupId(invitation.getGroupId())
                .inviterId(invitation.getInviterId())
                .inviteeEmail(invitation.getInviteeEmail())
                .status(invitation.getStatus())
                .token(invitation.getToken())
                .expiresAt(invitation.getExpiresAt())
                .createdAt(invitation.getCreatedAt())
                .build();
    }
}
