package com.splitter.user.service;

import com.splitter.user.exception.InvalidCredentialsException;
import com.splitter.user.exception.InvalidTokenException;
import com.splitter.user.model.RefreshToken;
import com.splitter.user.model.User;
import com.splitter.user.repository.RefreshTokenRepository;
import com.splitter.user.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * Service for authentication operations.
 */
@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final SecretKey jwtSecretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            UserService userService,
            PasswordEncoder passwordEncoder,
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.access-token-expiration:900}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration:604800}") long refreshTokenExpiration) {
        
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtSecretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * Authenticate user with email and password.
     */
    public Mono<AuthResponse> login(LoginRequest request, String deviceInfo, String ipAddress) {
        log.info("Login attempt for email: {}", request.email());

        return userRepository.findByEmail(request.email())
                .filter(user -> user.isActive())
                .filter(user -> passwordEncoder.matches(request.password(), user.getPasswordHash()))
                .switchIfEmpty(Mono.error(new InvalidCredentialsException()))
                .flatMap(user -> createAuthResponse(user, deviceInfo, ipAddress))
                .doOnSuccess(response -> userService.updateLastLogin(
                        UUID.fromString(extractUserIdFromToken(response.accessToken()))).subscribe());
    }

    /**
     * Refresh access token using refresh token.
     */
    public Mono<AuthResponse> refreshToken(String refreshTokenString, String deviceInfo, String ipAddress) {
        log.debug("Refreshing token");

        return refreshTokenRepository.findValidToken(refreshTokenString)
                .switchIfEmpty(Mono.error(new InvalidTokenException("Invalid or expired refresh token")))
                .flatMap(refreshToken -> userRepository.findById(refreshToken.getUserId()))
                .filter(User::isActive)
                .switchIfEmpty(Mono.error(new InvalidTokenException("User account is deactivated")))
                .flatMap(user -> {
                    // Revoke old refresh token
                    return refreshTokenRepository.revokeToken(refreshTokenString)
                            .then(createAuthResponse(user, deviceInfo, ipAddress));
                });
    }

    /**
     * Logout user by revoking refresh token.
     */
    public Mono<Void> logout(String refreshToken) {
        return refreshTokenRepository.revokeToken(refreshToken)
                .then();
    }

    /**
     * Logout from all devices by revoking all refresh tokens.
     */
    public Mono<Void> logoutAll(UUID userId) {
        log.info("Logging out user {} from all devices", userId);
        return refreshTokenRepository.revokeAllUserTokens(userId)
                .then();
    }

    /**
     * Validate access token and return user ID.
     */
    public Mono<UUID> validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtSecretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return Mono.just(UUID.fromString(claims.getSubject()));
        } catch (JwtException e) {
            return Mono.error(new InvalidTokenException("Invalid token"));
        }
    }

    // Private helper methods

    private Mono<AuthResponse> createAuthResponse(User user, String deviceInfo, String ipAddress) {
        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken();

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .userId(user.getId())
                .token(refreshToken)
                .expiresAt(Instant.now().plus(refreshTokenExpiration, ChronoUnit.SECONDS))
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .createdAt(Instant.now())
                .build();

        return refreshTokenRepository.save(refreshTokenEntity)
                .map(saved -> new AuthResponse(
                        accessToken,
                        refreshToken,
                        accessTokenExpiration,
                        "Bearer",
                        user.getId(),
                        user.getEmail(),
                        user.getDisplayName()
                ));
    }

    private String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(accessTokenExpiration, ChronoUnit.SECONDS);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("displayName", user.getDisplayName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(jwtSecretKey)
                .compact();
    }

    private String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    private String extractUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(jwtSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    /**
     * Login request record.
     */
    public record LoginRequest(String email, String password) {}

    /**
     * Authentication response record.
     */
    public record AuthResponse(
            String accessToken,
            String refreshToken,
            long expiresIn,
            String tokenType,
            UUID userId,
            String email,
            String displayName
    ) {}
}
