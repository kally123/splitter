package com.splitter.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Utility class for JWT token operations.
 */
@Slf4j
@Component
public class JwtUtils {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtUtils(
            @Value("${jwt.secret:your-256-bit-secret-key-here-change-in-production}") String secret,
            @Value("${jwt.expiration:86400000}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Generates a JWT token for a user.
     */
    public String generateToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationMs, ChronoUnit.MILLIS);

        return Jwts.builder()
                .subject(user.getUserId().toString())
                .claim("email", user.getEmail())
                .claim("name", user.getDisplayName())
                .claim("roles", user.getRoles())
                .claim("groups", user.getGroupIds() != null
                        ? user.getGroupIds().stream().map(UUID::toString).toList()
                        : List.of())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validates a JWT token and returns the claims.
     */
    public Optional<Claims> validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Extracts the authenticated user from a JWT token.
     */
    public Optional<AuthenticatedUser> extractUser(String token) {
        return validateToken(token).map(claims -> {
            List<String> roles = claims.get("roles", List.class);
            List<String> groupStrings = claims.get("groups", List.class);
            List<UUID> groups = groupStrings != null
                    ? groupStrings.stream().map(UUID::fromString).toList()
                    : List.of();

            return AuthenticatedUser.builder()
                    .userId(UUID.fromString(claims.getSubject()))
                    .email(claims.get("email", String.class))
                    .displayName(claims.get("name", String.class))
                    .roles(roles != null ? roles : List.of())
                    .groupIds(groups)
                    .build();
        });
    }

    /**
     * Extracts the user ID from a JWT token.
     */
    public Optional<UUID> extractUserId(String token) {
        return validateToken(token)
                .map(claims -> UUID.fromString(claims.getSubject()));
    }

    /**
     * Checks if a token is expired.
     */
    public boolean isTokenExpired(String token) {
        return validateToken(token)
                .map(claims -> claims.getExpiration().before(new Date()))
                .orElse(true);
    }
}
