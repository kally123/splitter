package com.splitter.user.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * User entity for storing user information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User {

    @Id
    private UUID id;

    @Column("email")
    private String email;

    @Column("password_hash")
    private String passwordHash;

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    @Column("display_name")
    private String displayName;

    @Column("phone_number")
    private String phoneNumber;

    @Column("avatar_url")
    private String avatarUrl;

    @Column("default_currency")
    @Builder.Default
    private String defaultCurrency = "USD";

    @Column("locale")
    @Builder.Default
    private String locale = "en-US";

    @Column("timezone")
    @Builder.Default
    private String timezone = "UTC";

    @Column("email_verified")
    @Builder.Default
    private boolean emailVerified = false;

    @Column("is_active")
    @Builder.Default
    private boolean active = true;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;

    @Column("last_login_at")
    private Instant lastLoginAt;

    /**
     * Get full name combining first and last name.
     */
    public String getFullName() {
        if (firstName == null && lastName == null) {
            return displayName;
        }
        return String.format("%s %s", 
            firstName != null ? firstName : "", 
            lastName != null ? lastName : "").trim();
    }
}
