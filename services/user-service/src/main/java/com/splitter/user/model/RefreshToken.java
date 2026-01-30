package com.splitter.user.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Refresh token entity for managing JWT refresh tokens.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("refresh_tokens")
public class RefreshToken {

    @Id
    private UUID id;

    @Column("user_id")
    private UUID userId;

    @Column("token")
    private String token;

    @Column("expires_at")
    private Instant expiresAt;

    @Column("device_info")
    private String deviceInfo;

    @Column("ip_address")
    private String ipAddress;

    @Column("is_revoked")
    @Builder.Default
    private boolean revoked = false;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    /**
     * Check if the token is expired.
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if the token is valid (not expired and not revoked).
     */
    public boolean isValid() {
        return !isExpired() && !revoked;
    }
}
