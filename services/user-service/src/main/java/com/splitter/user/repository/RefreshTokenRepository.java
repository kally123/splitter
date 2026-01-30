package com.splitter.user.repository;

import com.splitter.user.model.RefreshToken;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for RefreshToken entities.
 */
@Repository
public interface RefreshTokenRepository extends R2dbcRepository<RefreshToken, UUID> {

    /**
     * Find refresh token by token string.
     */
    Mono<RefreshToken> findByToken(String token);

    /**
     * Find valid (not revoked, not expired) token by token string.
     */
    @Query("SELECT * FROM refresh_tokens WHERE token = :token AND is_revoked = false AND expires_at > NOW()")
    Mono<RefreshToken> findValidToken(String token);

    /**
     * Revoke all tokens for a user.
     */
    @Modifying
    @Query("UPDATE refresh_tokens SET is_revoked = true WHERE user_id = :userId AND is_revoked = false")
    Mono<Integer> revokeAllUserTokens(UUID userId);

    /**
     * Revoke a specific token.
     */
    @Modifying
    @Query("UPDATE refresh_tokens SET is_revoked = true WHERE token = :token")
    Mono<Integer> revokeToken(String token);

    /**
     * Delete expired tokens (cleanup job).
     */
    @Modifying
    @Query("DELETE FROM refresh_tokens WHERE expires_at < NOW() OR is_revoked = true")
    Mono<Integer> deleteExpiredTokens();

    /**
     * Count active tokens for a user.
     */
    @Query("SELECT COUNT(*) FROM refresh_tokens WHERE user_id = :userId AND is_revoked = false AND expires_at > NOW()")
    Mono<Long> countActiveTokens(UUID userId);
}
