package com.splitter.settlement.service;

import com.splitter.common.events.EventTopics;
import com.splitter.common.events.settlement.SettlementCreatedEvent;
import com.splitter.settlement.dto.CreateSettlementRequest;
import com.splitter.settlement.dto.SettlementDto;
import com.splitter.settlement.exception.SettlementNotFoundException;
import com.splitter.settlement.exception.UnauthorizedSettlementActionException;
import com.splitter.settlement.model.Settlement;
import com.splitter.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Service for settlement management operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Create a new settlement (payment record).
     */
    @Transactional
    public Mono<SettlementDto> createSettlement(CreateSettlementRequest request, UUID fromUserId) {
        log.info("Creating settlement from {} to {} for amount {}", 
                fromUserId, request.toUserId(), request.amount());

        if (fromUserId.equals(request.toUserId())) {
            return Mono.error(new IllegalArgumentException("Cannot create settlement to yourself"));
        }

        Settlement settlement = Settlement.builder()
                .groupId(request.groupId())
                .fromUserId(fromUserId)
                .toUserId(request.toUserId())
                .amount(request.amount())
                .currency(request.currency() != null ? request.currency() : "USD")
                .paymentMethod(request.paymentMethod() != null ? 
                        request.paymentMethod() : Settlement.PaymentMethod.OTHER)
                .status(Settlement.SettlementStatus.PENDING)
                .settlementDate(request.settlementDate() != null ? 
                        request.settlementDate() : LocalDate.now())
                .notes(request.notes())
                .externalReference(request.externalReference())
                .createdBy(fromUserId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return settlementRepository.save(settlement)
                .map(this::toDto);
    }

    /**
     * Get settlement by ID.
     */
    public Mono<SettlementDto> getSettlementById(UUID settlementId) {
        return settlementRepository.findById(settlementId)
                .switchIfEmpty(Mono.error(new SettlementNotFoundException(settlementId)))
                .map(this::toDto);
    }

    /**
     * Get settlements for a group.
     */
    public Flux<SettlementDto> getGroupSettlements(UUID groupId) {
        return settlementRepository.findByGroupIdOrderByCreatedAtDesc(groupId)
                .map(this::toDto);
    }

    /**
     * Get settlements involving a user.
     */
    public Flux<SettlementDto> getUserSettlements(UUID userId) {
        return settlementRepository.findByUser(userId)
                .map(this::toDto);
    }

    /**
     * Get pending settlements for user to confirm.
     */
    public Flux<SettlementDto> getPendingSettlements(UUID userId) {
        return settlementRepository.findByToUserIdAndStatus(userId, Settlement.SettlementStatus.PENDING)
                .map(this::toDto);
    }

    /**
     * Confirm a settlement.
     */
    @Transactional
    public Mono<SettlementDto> confirmSettlement(UUID settlementId, UUID confirmerId) {
        return settlementRepository.findById(settlementId)
                .switchIfEmpty(Mono.error(new SettlementNotFoundException(settlementId)))
                .flatMap(settlement -> {
                    // Only the recipient can confirm
                    if (!settlement.getToUserId().equals(confirmerId)) {
                        return Mono.error(new UnauthorizedSettlementActionException(
                                "Only the recipient can confirm a settlement"));
                    }
                    
                    if (settlement.getStatus() != Settlement.SettlementStatus.PENDING) {
                        return Mono.error(new IllegalStateException(
                                "Only pending settlements can be confirmed"));
                    }

                    settlement.setStatus(Settlement.SettlementStatus.CONFIRMED);
                    settlement.setConfirmedBy(confirmerId);
                    settlement.setConfirmedAt(Instant.now());
                    settlement.setUpdatedAt(Instant.now());

                    return settlementRepository.save(settlement);
                })
                .doOnSuccess(this::publishSettlementConfirmedEvent)
                .map(this::toDto);
    }

    /**
     * Reject a settlement.
     */
    @Transactional
    public Mono<SettlementDto> rejectSettlement(UUID settlementId, UUID rejecterId, String reason) {
        return settlementRepository.findById(settlementId)
                .switchIfEmpty(Mono.error(new SettlementNotFoundException(settlementId)))
                .flatMap(settlement -> {
                    // Only the recipient can reject
                    if (!settlement.getToUserId().equals(rejecterId)) {
                        return Mono.error(new UnauthorizedSettlementActionException(
                                "Only the recipient can reject a settlement"));
                    }

                    if (settlement.getStatus() != Settlement.SettlementStatus.PENDING) {
                        return Mono.error(new IllegalStateException(
                                "Only pending settlements can be rejected"));
                    }

                    settlement.setStatus(Settlement.SettlementStatus.REJECTED);
                    settlement.setNotes(settlement.getNotes() != null ? 
                            settlement.getNotes() + " | Rejection reason: " + reason : 
                            "Rejection reason: " + reason);
                    settlement.setUpdatedAt(Instant.now());

                    return settlementRepository.save(settlement);
                })
                .map(this::toDto);
    }

    /**
     * Cancel a settlement (by the creator).
     */
    @Transactional
    public Mono<SettlementDto> cancelSettlement(UUID settlementId, UUID requesterId) {
        return settlementRepository.findById(settlementId)
                .switchIfEmpty(Mono.error(new SettlementNotFoundException(settlementId)))
                .flatMap(settlement -> {
                    // Only the creator can cancel
                    if (!settlement.getCreatedBy().equals(requesterId)) {
                        return Mono.error(new UnauthorizedSettlementActionException(
                                "Only the creator can cancel a settlement"));
                    }

                    if (settlement.getStatus() != Settlement.SettlementStatus.PENDING) {
                        return Mono.error(new IllegalStateException(
                                "Only pending settlements can be cancelled"));
                    }

                    settlement.setStatus(Settlement.SettlementStatus.CANCELLED);
                    settlement.setUpdatedAt(Instant.now());

                    return settlementRepository.save(settlement);
                })
                .map(this::toDto);
    }

    /**
     * Get settlement statistics for a group.
     */
    public Mono<BigDecimal> getTotalSettledAmount(UUID groupId) {
        return settlementRepository.sumConfirmedByGroupId(groupId);
    }

    /**
     * Count pending settlements for a user.
     */
    public Mono<Long> countPendingSettlements(UUID userId) {
        return settlementRepository.countByToUserIdAndStatus(userId, Settlement.SettlementStatus.PENDING);
    }

    private void publishSettlementConfirmedEvent(Settlement settlement) {
        SettlementCreatedEvent event = SettlementCreatedEvent.builder()
                .settlementId(settlement.getId())
                .groupId(settlement.getGroupId())
                .fromUserId(settlement.getFromUserId())
                .toUserId(settlement.getToUserId())
                .amount(settlement.getAmount())
                .currency(settlement.getCurrency())
                .build();

        kafkaTemplate.send(EventTopics.SETTLEMENT_EVENTS, settlement.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish SettlementCreatedEvent for settlement: {}", 
                                settlement.getId(), ex);
                    } else {
                        log.info("Published SettlementCreatedEvent for settlement: {}", settlement.getId());
                    }
                });
    }

    private SettlementDto toDto(Settlement settlement) {
        return SettlementDto.builder()
                .id(settlement.getId())
                .groupId(settlement.getGroupId())
                .fromUserId(settlement.getFromUserId())
                .toUserId(settlement.getToUserId())
                .amount(settlement.getAmount())
                .currency(settlement.getCurrency())
                .paymentMethod(settlement.getPaymentMethod())
                .status(settlement.getStatus())
                .settlementDate(settlement.getSettlementDate())
                .notes(settlement.getNotes())
                .externalReference(settlement.getExternalReference())
                .createdBy(settlement.getCreatedBy())
                .confirmedBy(settlement.getConfirmedBy())
                .confirmedAt(settlement.getConfirmedAt())
                .createdAt(settlement.getCreatedAt())
                .updatedAt(settlement.getUpdatedAt())
                .build();
    }
}
