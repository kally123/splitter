package com.splitter.notification.controller;

import com.splitter.notification.dto.NotificationDto;
import com.splitter.notification.dto.NotificationPreferenceDto;
import com.splitter.notification.dto.UpdatePreferencesRequest;
import com.splitter.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST controller for notification operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management operations")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get notifications for the current user")
    public Flux<NotificationDto> getNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return notificationService.getUserNotifications(userId, page, Math.min(size, 100));
    }

    @GetMapping("/unread")
    @Operation(summary = "Get unread notifications")
    public Flux<NotificationDto> getUnreadNotifications(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return notificationService.getUnreadNotifications(userId);
    }

    @GetMapping("/unread/count")
    @Operation(summary = "Get unread notification count")
    public Mono<Long> getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return notificationService.getUnreadCount(userId);
    }

    @PostMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mark notification as read")
    public Mono<Void> markAsRead(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return notificationService.markAsRead(notificationId, userId);
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public Mono<Integer> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return notificationService.markAllAsRead(userId);
    }

    @GetMapping("/preferences")
    @Operation(summary = "Get notification preferences")
    public Mono<NotificationPreferenceDto> getPreferences(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return notificationService.getPreferences(userId);
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update notification preferences")
    public Mono<NotificationPreferenceDto> updatePreferences(
            @RequestBody UpdatePreferencesRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return notificationService.updatePreferences(userId, request);
    }
}
