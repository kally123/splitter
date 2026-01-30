package com.splitter.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitter.notification.dto.NotificationDto;
import com.splitter.notification.dto.NotificationPreferenceDto;
import com.splitter.notification.dto.UpdatePreferencesRequest;
import com.splitter.notification.model.Notification;
import com.splitter.notification.model.NotificationPreference;
import com.splitter.notification.repository.NotificationPreferenceRepository;
import com.splitter.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

/**
 * Service for notification management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    /**
     * Create and send a notification.
     */
    @Transactional
    public Mono<NotificationDto> createNotification(
            UUID userId,
            Notification.NotificationType type,
            Notification.NotificationChannel channel,
            String title,
            String message,
            Map<String, Object> data,
            String referenceType,
            UUID referenceId) {

        String dataJson;
        try {
            dataJson = data != null ? objectMapper.writeValueAsString(data) : null;
        } catch (Exception e) {
            dataJson = null;
        }

        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .channel(channel)
                .title(title)
                .message(message)
                .data(dataJson)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .status(Notification.NotificationStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        return notificationRepository.save(notification)
                .flatMap(saved -> {
                    if (channel == Notification.NotificationChannel.IN_APP) {
                        // Mark as sent immediately for in-app
                        saved.setStatus(Notification.NotificationStatus.SENT);
                        saved.setSentAt(Instant.now());
                        return notificationRepository.save(saved);
                    }
                    return Mono.just(saved);
                })
                .map(this::toDto);
    }

    /**
     * Get notifications for a user.
     */
    public Flux<NotificationDto> getUserNotifications(UUID userId, int page, int size) {
        return notificationRepository.findByUserId(userId, size, page * size)
                .map(this::toDto);
    }

    /**
     * Get unread notifications for a user.
     */
    public Flux<NotificationDto> getUnreadNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                .map(this::toDto);
    }

    /**
     * Get unread notification count.
     */
    public Mono<Long> getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    /**
     * Mark notification as read.
     */
    public Mono<Void> markAsRead(UUID notificationId, UUID userId) {
        return notificationRepository.findById(notificationId)
                .filter(n -> n.getUserId().equals(userId))
                .flatMap(n -> notificationRepository.markAsRead(notificationId, Instant.now()))
                .then();
    }

    /**
     * Mark all notifications as read.
     */
    public Mono<Integer> markAllAsRead(UUID userId) {
        return notificationRepository.markAllAsRead(userId, Instant.now());
    }

    /**
     * Get notification preferences.
     */
    public Mono<NotificationPreferenceDto> getPreferences(UUID userId) {
        return preferenceRepository.findByUserId(userId)
                .switchIfEmpty(createDefaultPreferences(userId))
                .map(this::toPreferenceDto);
    }

    /**
     * Update notification preferences.
     */
    @Transactional
    public Mono<NotificationPreferenceDto> updatePreferences(UUID userId, UpdatePreferencesRequest request) {
        return preferenceRepository.findByUserId(userId)
                .switchIfEmpty(createDefaultPreferences(userId))
                .flatMap(prefs -> {
                    if (request.emailExpenseAdded() != null) 
                        prefs.setEmailExpenseAdded(request.emailExpenseAdded());
                    if (request.emailSettlementRequested() != null) 
                        prefs.setEmailSettlementRequested(request.emailSettlementRequested());
                    if (request.emailSettlementConfirmed() != null) 
                        prefs.setEmailSettlementConfirmed(request.emailSettlementConfirmed());
                    if (request.emailGroupInvitation() != null) 
                        prefs.setEmailGroupInvitation(request.emailGroupInvitation());
                    if (request.emailReminders() != null) 
                        prefs.setEmailReminders(request.emailReminders());
                    if (request.emailWeeklySummary() != null) 
                        prefs.setEmailWeeklySummary(request.emailWeeklySummary());
                    if (request.pushEnabled() != null) 
                        prefs.setPushEnabled(request.pushEnabled());
                    if (request.pushExpenseAdded() != null) 
                        prefs.setPushExpenseAdded(request.pushExpenseAdded());
                    if (request.pushSettlementRequested() != null) 
                        prefs.setPushSettlementRequested(request.pushSettlementRequested());
                    if (request.pushSettlementConfirmed() != null) 
                        prefs.setPushSettlementConfirmed(request.pushSettlementConfirmed());
                    if (request.inAppEnabled() != null) 
                        prefs.setInAppEnabled(request.inAppEnabled());
                    prefs.setUpdatedAt(Instant.now());
                    return preferenceRepository.save(prefs);
                })
                .map(this::toPreferenceDto);
    }

    private Mono<NotificationPreference> createDefaultPreferences(UUID userId) {
        NotificationPreference prefs = NotificationPreference.builder()
                .userId(userId)
                .updatedAt(Instant.now())
                .build();
        return preferenceRepository.save(prefs);
    }

    /**
     * Cleanup old read notifications.
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    public void cleanupOldNotifications() {
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        notificationRepository.deleteOldReadNotifications(thirtyDaysAgo)
                .subscribe(count -> log.info("Cleaned up {} old notifications", count));
    }

    private NotificationDto toDto(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .channel(notification.getChannel())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .data(notification.getData())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .status(notification.getStatus())
                .read(notification.isRead())
                .readAt(notification.getReadAt())
                .sentAt(notification.getSentAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private NotificationPreferenceDto toPreferenceDto(NotificationPreference pref) {
        return NotificationPreferenceDto.builder()
                .userId(pref.getUserId())
                .emailExpenseAdded(pref.isEmailExpenseAdded())
                .emailSettlementRequested(pref.isEmailSettlementRequested())
                .emailSettlementConfirmed(pref.isEmailSettlementConfirmed())
                .emailGroupInvitation(pref.isEmailGroupInvitation())
                .emailReminders(pref.isEmailReminders())
                .emailWeeklySummary(pref.isEmailWeeklySummary())
                .pushEnabled(pref.isPushEnabled())
                .pushExpenseAdded(pref.isPushExpenseAdded())
                .pushSettlementRequested(pref.isPushSettlementRequested())
                .pushSettlementConfirmed(pref.isPushSettlementConfirmed())
                .inAppEnabled(pref.isInAppEnabled())
                .updatedAt(pref.getUpdatedAt())
                .build();
    }
}
