package com.splitter.user.controller;

import com.splitter.common.dto.user.CreateUserRequest;
import com.splitter.common.dto.user.UserDto;
import com.splitter.user.service.AuthService;
import com.splitter.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST controller for authentication operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and token management")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register new user", description = "Create a new user account and return authentication tokens")
    public Mono<AuthService.AuthResponse> register(
            @Valid @RequestBody CreateUserRequest request,
            ServerHttpRequest httpRequest) {
        log.info("Registration request for email: {}", request.email());

        String deviceInfo = httpRequest.getHeaders().getFirst("User-Agent");
        String ipAddress = getClientIpAddress(httpRequest);

        return userService.createUser(request)
                .flatMap(user -> authService.login(
                        new AuthService.LoginRequest(request.email(), request.password()),
                        deviceInfo,
                        ipAddress
                ));
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate user and return access and refresh tokens")
    public Mono<AuthService.AuthResponse> login(
            @Valid @RequestBody AuthService.LoginRequest request,
            ServerHttpRequest httpRequest) {
        log.info("Login request for email: {}", request.email());

        String deviceInfo = httpRequest.getHeaders().getFirst("User-Agent");
        String ipAddress = getClientIpAddress(httpRequest);

        return authService.login(request, deviceInfo, ipAddress);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Get new access token using refresh token")
    public Mono<AuthService.AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            ServerHttpRequest httpRequest) {
        log.debug("Token refresh request");

        String deviceInfo = httpRequest.getHeaders().getFirst("User-Agent");
        String ipAddress = getClientIpAddress(httpRequest);

        return authService.refreshToken(request.refreshToken(), deviceInfo, ipAddress);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Logout", description = "Revoke the current refresh token")
    public Mono<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        log.debug("Logout request");
        return authService.logout(request.refreshToken());
    }

    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Logout from all devices", description = "Revoke all refresh tokens for the current user")
    public Mono<Void> logoutAll(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("Logout all devices for user: {}", userId);
        return authService.logoutAll(userId);
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate token", description = "Validate the current access token")
    public Mono<TokenValidationResponse> validateToken(@AuthenticationPrincipal Jwt jwt) {
        return Mono.just(new TokenValidationResponse(
                true,
                UUID.fromString(jwt.getSubject()),
                jwt.getClaimAsString("email")
        ));
    }

    // Helper methods

    private String getClientIpAddress(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddress() != null 
                ? request.getRemoteAddress().getAddress().getHostAddress() 
                : "unknown";
    }

    /**
     * Refresh token request record.
     */
    public record RefreshTokenRequest(String refreshToken) {}

    /**
     * Token validation response record.
     */
    public record TokenValidationResponse(boolean valid, UUID userId, String email) {}
}
