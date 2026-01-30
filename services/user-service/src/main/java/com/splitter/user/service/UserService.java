package com.splitter.user.service;

import com.splitter.common.dto.user.CreateUserRequest;
import com.splitter.common.dto.user.UserDto;
import com.splitter.common.events.EventTopics;
import com.splitter.common.events.user.UserCreatedEvent;
import com.splitter.user.exception.EmailAlreadyExistsException;
import com.splitter.user.exception.UserNotFoundException;
import com.splitter.user.model.User;
import com.splitter.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

/**
 * Service for user management operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Create a new user.
     */
    @Transactional
    public Mono<UserDto> createUser(CreateUserRequest request) {
        log.info("Creating user with email: {}", request.email());

        return userRepository.existsByEmail(request.email())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new EmailAlreadyExistsException(request.email()));
                    }

                    User user = User.builder()
                            .email(request.email())
                            .passwordHash(passwordEncoder.encode(request.password()))
                            .firstName(request.firstName())
                            .lastName(request.lastName())
                            .displayName(buildDisplayName(request))
                            .defaultCurrency(request.defaultCurrency() != null ? request.defaultCurrency() : "USD")
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();

                    return userRepository.save(user);
                })
                .doOnSuccess(user -> publishUserCreatedEvent(user))
                .map(this::toDto);
    }

    /**
     * Get user by ID.
     */
    public Mono<UserDto> getUserById(UUID userId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException(userId)))
                .map(this::toDto);
    }

    /**
     * Get user by email.
     */
    public Mono<UserDto> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new UserNotFoundException("email", email)))
                .map(this::toDto);
    }

    /**
     * Get users by IDs.
     */
    public Flux<UserDto> getUsersByIds(Collection<UUID> userIds) {
        return userRepository.findByIdIn(userIds)
                .map(this::toDto);
    }

    /**
     * Search users by email or display name.
     */
    public Flux<UserDto> searchUsers(String searchTerm, int limit) {
        return userRepository.searchUsers(searchTerm, limit)
                .map(this::toDto);
    }

    /**
     * Update user profile.
     */
    @Transactional
    public Mono<UserDto> updateUser(UUID userId, UpdateUserRequest request) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException(userId)))
                .map(user -> {
                    if (request.firstName() != null) {
                        user.setFirstName(request.firstName());
                    }
                    if (request.lastName() != null) {
                        user.setLastName(request.lastName());
                    }
                    if (request.displayName() != null) {
                        user.setDisplayName(request.displayName());
                    }
                    if (request.phoneNumber() != null) {
                        user.setPhoneNumber(request.phoneNumber());
                    }
                    if (request.avatarUrl() != null) {
                        user.setAvatarUrl(request.avatarUrl());
                    }
                    if (request.defaultCurrency() != null) {
                        user.setDefaultCurrency(request.defaultCurrency());
                    }
                    if (request.locale() != null) {
                        user.setLocale(request.locale());
                    }
                    if (request.timezone() != null) {
                        user.setTimezone(request.timezone());
                    }
                    user.setUpdatedAt(Instant.now());
                    return user;
                })
                .flatMap(userRepository::save)
                .map(this::toDto);
    }

    /**
     * Update last login timestamp.
     */
    public Mono<Void> updateLastLogin(UUID userId) {
        return userRepository.findById(userId)
                .flatMap(user -> {
                    user.setLastLoginAt(Instant.now());
                    return userRepository.save(user);
                })
                .then();
    }

    /**
     * Deactivate user account.
     */
    @Transactional
    public Mono<Void> deactivateUser(UUID userId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException(userId)))
                .flatMap(user -> {
                    user.setActive(false);
                    user.setUpdatedAt(Instant.now());
                    return userRepository.save(user);
                })
                .then();
    }

    /**
     * Get internal user entity (for authentication).
     */
    public Mono<User> getInternalUser(String email) {
        return userRepository.findByEmail(email);
    }

    // Private helper methods

    private String buildDisplayName(CreateUserRequest request) {
        if (request.displayName() != null && !request.displayName().isBlank()) {
            return request.displayName();
        }
        String firstName = request.firstName() != null ? request.firstName() : "";
        String lastName = request.lastName() != null ? request.lastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? request.email().split("@")[0] : fullName;
    }

    private void publishUserCreatedEvent(User user) {
        UserCreatedEvent event = UserCreatedEvent.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .defaultCurrency(user.getDefaultCurrency())
                .build();

        kafkaTemplate.send(EventTopics.USER_EVENTS, user.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish UserCreatedEvent for user: {}", user.getId(), ex);
                    } else {
                        log.info("Published UserCreatedEvent for user: {}", user.getId());
                    }
                });
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .displayName(user.getDisplayName())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .defaultCurrency(user.getDefaultCurrency())
                .locale(user.getLocale())
                .timezone(user.getTimezone())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Request record for updating user profile.
     */
    public record UpdateUserRequest(
            String firstName,
            String lastName,
            String displayName,
            String phoneNumber,
            String avatarUrl,
            String defaultCurrency,
            String locale,
            String timezone
    ) {}
}
