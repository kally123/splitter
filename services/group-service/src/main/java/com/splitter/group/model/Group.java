package com.splitter.group.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Group entity representing a collection of users sharing expenses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("groups")
public class Group {

    @Id
    private UUID id;

    @Column("name")
    private String name;

    @Column("description")
    private String description;

    @Column("group_type")
    private GroupType type;

    @Column("default_currency")
    @Builder.Default
    private String defaultCurrency = "USD";

    @Column("cover_image_url")
    private String coverImageUrl;

    @Column("created_by")
    private UUID createdBy;

    @Column("is_active")
    @Builder.Default
    private boolean active = true;

    @Column("simplify_debts")
    @Builder.Default
    private boolean simplifyDebts = true;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;

    /**
     * Group types for categorization.
     */
    public enum GroupType {
        HOME,
        TRIP,
        COUPLE,
        FRIENDS,
        FAMILY,
        WORK,
        OTHER
    }
}
