package com.splitter.group.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * GroupMember entity representing a user's membership in a group.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("group_members")
public class GroupMember {

    @Id
    private UUID id;

    @Column("group_id")
    private UUID groupId;

    @Column("user_id")
    private UUID userId;

    @Column("role")
    @Builder.Default
    private MemberRole role = MemberRole.MEMBER;

    @Column("display_name")
    private String displayName;

    @Column("is_active")
    @Builder.Default
    private boolean active = true;

    @CreatedDate
    @Column("joined_at")
    private Instant joinedAt;

    @Column("left_at")
    private Instant leftAt;

    /**
     * Member roles in a group.
     */
    public enum MemberRole {
        OWNER,
        ADMIN,
        MEMBER
    }
}
