package com.splitter.group.service;

import com.splitter.common.events.EventTopics;
import com.splitter.common.events.group.GroupCreatedEvent;
import com.splitter.group.dto.*;
import com.splitter.group.exception.GroupNotFoundException;
import com.splitter.group.exception.NotGroupMemberException;
import com.splitter.group.exception.UnauthorizedGroupActionException;
import com.splitter.group.model.Group;
import com.splitter.group.model.GroupMember;
import com.splitter.group.repository.GroupMemberRepository;
import com.splitter.group.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for group management operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Create a new group.
     */
    @Transactional
    public Mono<GroupDto> createGroup(CreateGroupRequest request, UUID creatorId) {
        log.info("Creating group '{}' by user {}", request.name(), creatorId);

        Group group = Group.builder()
                .name(request.name())
                .description(request.description())
                .type(request.type() != null ? request.type() : Group.GroupType.OTHER)
                .defaultCurrency(request.defaultCurrency() != null ? request.defaultCurrency() : "USD")
                .coverImageUrl(request.coverImageUrl())
                .createdBy(creatorId)
                .simplifyDebts(request.simplifyDebts() != null ? request.simplifyDebts() : true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return groupRepository.save(group)
                .flatMap(savedGroup -> addCreatorAsMember(savedGroup, creatorId)
                        .thenReturn(savedGroup))
                .doOnSuccess(this::publishGroupCreatedEvent)
                .flatMap(this::enrichWithMemberCount);
    }

    /**
     * Get group by ID.
     */
    public Mono<GroupDto> getGroupById(UUID groupId, UUID requesterId) {
        return groupRepository.findById(groupId)
                .filter(Group::isActive)
                .switchIfEmpty(Mono.error(new GroupNotFoundException(groupId)))
                .flatMap(group -> verifyMembership(groupId, requesterId)
                        .thenReturn(group))
                .flatMap(this::enrichWithMemberCount);
    }

    /**
     * Get all groups for a user.
     */
    public Flux<GroupDto> getUserGroups(UUID userId) {
        return memberRepository.findGroupIdsByUserId(userId)
                .flatMap(groupRepository::findById)
                .filter(Group::isActive)
                .flatMap(this::enrichWithMemberCount);
    }

    /**
     * Update a group.
     */
    @Transactional
    public Mono<GroupDto> updateGroup(UUID groupId, UpdateGroupRequest request, UUID requesterId) {
        return groupRepository.findById(groupId)
                .filter(Group::isActive)
                .switchIfEmpty(Mono.error(new GroupNotFoundException(groupId)))
                .flatMap(group -> verifyAdminAccess(groupId, requesterId).thenReturn(group))
                .map(group -> {
                    if (request.name() != null) group.setName(request.name());
                    if (request.description() != null) group.setDescription(request.description());
                    if (request.type() != null) group.setType(request.type());
                    if (request.defaultCurrency() != null) group.setDefaultCurrency(request.defaultCurrency());
                    if (request.coverImageUrl() != null) group.setCoverImageUrl(request.coverImageUrl());
                    if (request.simplifyDebts() != null) group.setSimplifyDebts(request.simplifyDebts());
                    group.setUpdatedAt(Instant.now());
                    return group;
                })
                .flatMap(groupRepository::save)
                .flatMap(this::enrichWithMemberCount);
    }

    /**
     * Delete (deactivate) a group.
     */
    @Transactional
    public Mono<Void> deleteGroup(UUID groupId, UUID requesterId) {
        return groupRepository.findById(groupId)
                .filter(Group::isActive)
                .switchIfEmpty(Mono.error(new GroupNotFoundException(groupId)))
                .flatMap(group -> {
                    if (!group.getCreatedBy().equals(requesterId)) {
                        return Mono.error(new UnauthorizedGroupActionException("Only the group owner can delete the group"));
                    }
                    group.setActive(false);
                    group.setUpdatedAt(Instant.now());
                    return groupRepository.save(group);
                })
                .then();
    }

    /**
     * Get group members.
     */
    public Flux<GroupMemberDto> getGroupMembers(UUID groupId, UUID requesterId) {
        return verifyMembership(groupId, requesterId)
                .thenMany(memberRepository.findByGroupIdAndActiveTrue(groupId))
                .map(this::toMemberDto);
    }

    /**
     * Add a member to a group.
     */
    @Transactional
    public Mono<GroupMemberDto> addMember(UUID groupId, UUID userId, String displayName, UUID requesterId) {
        return verifyAdminAccess(groupId, requesterId)
                .then(memberRepository.existsByGroupIdAndUserIdAndActiveTrue(groupId, userId))
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalArgumentException("User is already a member of this group"));
                    }
                    GroupMember member = GroupMember.builder()
                            .groupId(groupId)
                            .userId(userId)
                            .displayName(displayName)
                            .role(GroupMember.MemberRole.MEMBER)
                            .joinedAt(Instant.now())
                            .build();
                    return memberRepository.save(member);
                })
                .map(this::toMemberDto);
    }

    /**
     * Remove a member from a group.
     */
    @Transactional
    public Mono<Void> removeMember(UUID groupId, UUID userId, UUID requesterId) {
        return verifyAdminAccess(groupId, requesterId)
                .then(memberRepository.findByGroupIdAndUserIdAndActiveTrue(groupId, userId))
                .switchIfEmpty(Mono.error(new NotGroupMemberException(groupId, userId)))
                .flatMap(member -> {
                    if (member.getRole() == GroupMember.MemberRole.OWNER) {
                        return Mono.error(new UnauthorizedGroupActionException("Cannot remove the group owner"));
                    }
                    return memberRepository.deactivateMembership(groupId, userId);
                })
                .then();
    }

    /**
     * Leave a group.
     */
    @Transactional
    public Mono<Void> leaveGroup(UUID groupId, UUID userId) {
        return memberRepository.findByGroupIdAndUserIdAndActiveTrue(groupId, userId)
                .switchIfEmpty(Mono.error(new NotGroupMemberException(groupId, userId)))
                .flatMap(member -> {
                    if (member.getRole() == GroupMember.MemberRole.OWNER) {
                        return Mono.error(new UnauthorizedGroupActionException("Owner cannot leave the group. Transfer ownership first."));
                    }
                    return memberRepository.deactivateMembership(groupId, userId);
                })
                .then();
    }

    /**
     * Update member role.
     */
    @Transactional
    public Mono<GroupMemberDto> updateMemberRole(UUID groupId, UUID userId, GroupMember.MemberRole newRole, UUID requesterId) {
        return verifyOwnerAccess(groupId, requesterId)
                .then(memberRepository.findByGroupIdAndUserIdAndActiveTrue(groupId, userId))
                .switchIfEmpty(Mono.error(new NotGroupMemberException(groupId, userId)))
                .flatMap(member -> {
                    member.setRole(newRole);
                    return memberRepository.save(member);
                })
                .map(this::toMemberDto);
    }

    // Private helper methods

    private Mono<GroupMember> addCreatorAsMember(Group group, UUID creatorId) {
        GroupMember member = GroupMember.builder()
                .groupId(group.getId())
                .userId(creatorId)
                .role(GroupMember.MemberRole.OWNER)
                .joinedAt(Instant.now())
                .build();
        return memberRepository.save(member);
    }

    private Mono<Void> verifyMembership(UUID groupId, UUID userId) {
        return memberRepository.existsByGroupIdAndUserIdAndActiveTrue(groupId, userId)
                .flatMap(isMember -> {
                    if (!isMember) {
                        return Mono.error(new NotGroupMemberException(groupId, userId));
                    }
                    return Mono.empty();
                });
    }

    private Mono<Void> verifyAdminAccess(UUID groupId, UUID userId) {
        return memberRepository.findByGroupIdAndUserIdAndActiveTrue(groupId, userId)
                .switchIfEmpty(Mono.error(new NotGroupMemberException(groupId, userId)))
                .flatMap(member -> {
                    if (member.getRole() != GroupMember.MemberRole.OWNER && 
                        member.getRole() != GroupMember.MemberRole.ADMIN) {
                        return Mono.error(new UnauthorizedGroupActionException("Admin access required"));
                    }
                    return Mono.empty();
                });
    }

    private Mono<Void> verifyOwnerAccess(UUID groupId, UUID userId) {
        return memberRepository.findByGroupIdAndUserIdAndActiveTrue(groupId, userId)
                .switchIfEmpty(Mono.error(new NotGroupMemberException(groupId, userId)))
                .flatMap(member -> {
                    if (member.getRole() != GroupMember.MemberRole.OWNER) {
                        return Mono.error(new UnauthorizedGroupActionException("Owner access required"));
                    }
                    return Mono.empty();
                });
    }

    private void publishGroupCreatedEvent(Group group) {
        GroupCreatedEvent event = GroupCreatedEvent.builder()
                .groupId(group.getId())
                .name(group.getName())
                .createdBy(group.getCreatedBy())
                .defaultCurrency(group.getDefaultCurrency())
                .build();

        kafkaTemplate.send(EventTopics.GROUP_EVENTS, group.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish GroupCreatedEvent for group: {}", group.getId(), ex);
                    } else {
                        log.info("Published GroupCreatedEvent for group: {}", group.getId());
                    }
                });
    }

    private Mono<GroupDto> enrichWithMemberCount(Group group) {
        return memberRepository.countByGroupIdAndActiveTrue(group.getId())
                .map(count -> toDto(group, count.intValue()));
    }

    private GroupDto toDto(Group group, int memberCount) {
        return GroupDto.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .type(group.getType())
                .defaultCurrency(group.getDefaultCurrency())
                .coverImageUrl(group.getCoverImageUrl())
                .createdBy(group.getCreatedBy())
                .simplifyDebts(group.isSimplifyDebts())
                .memberCount(memberCount)
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }

    private GroupMemberDto toMemberDto(GroupMember member) {
        return GroupMemberDto.builder()
                .id(member.getId())
                .groupId(member.getGroupId())
                .userId(member.getUserId())
                .displayName(member.getDisplayName())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
