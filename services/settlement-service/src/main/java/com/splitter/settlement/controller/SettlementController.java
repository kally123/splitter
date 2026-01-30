package com.splitter.settlement.controller;

import com.splitter.settlement.dto.CreateSettlementRequest;
import com.splitter.settlement.dto.SettlementDto;
import com.splitter.settlement.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * REST controller for settlement operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
@Tag(name = "Settlements", description = "Settlement and payment management operations")
public class SettlementController {

    private final SettlementService settlementService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new settlement")
    public Mono<SettlementDto> createSettlement(
            @Valid @RequestBody CreateSettlementRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("Creating settlement from user {} to {}", userId, request.toUserId());
        return settlementService.createSettlement(request, userId);
    }

    @GetMapping("/{settlementId}")
    @Operation(summary = "Get settlement by ID")
    public Mono<SettlementDto> getSettlement(@PathVariable UUID settlementId) {
        return settlementService.getSettlementById(settlementId);
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "Get settlements for a group")
    public Flux<SettlementDto> getGroupSettlements(@PathVariable UUID groupId) {
        return settlementService.getGroupSettlements(groupId);
    }

    @GetMapping("/user")
    @Operation(summary = "Get settlements for the current user")
    public Flux<SettlementDto> getCurrentUserSettlements(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return settlementService.getUserSettlements(userId);
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending settlements awaiting confirmation")
    public Flux<SettlementDto> getPendingSettlements(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return settlementService.getPendingSettlements(userId);
    }

    @PostMapping("/{settlementId}/confirm")
    @Operation(summary = "Confirm a settlement")
    public Mono<SettlementDto> confirmSettlement(
            @PathVariable UUID settlementId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} confirming settlement {}", userId, settlementId);
        return settlementService.confirmSettlement(settlementId, userId);
    }

    @PostMapping("/{settlementId}/reject")
    @Operation(summary = "Reject a settlement")
    public Mono<SettlementDto> rejectSettlement(
            @PathVariable UUID settlementId,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} rejecting settlement {}", userId, settlementId);
        return settlementService.rejectSettlement(settlementId, userId, reason);
    }

    @PostMapping("/{settlementId}/cancel")
    @Operation(summary = "Cancel a settlement")
    public Mono<SettlementDto> cancelSettlement(
            @PathVariable UUID settlementId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} cancelling settlement {}", userId, settlementId);
        return settlementService.cancelSettlement(settlementId, userId);
    }

    @GetMapping("/group/{groupId}/total")
    @Operation(summary = "Get total settled amount for a group")
    public Mono<BigDecimal> getTotalSettled(@PathVariable UUID groupId) {
        return settlementService.getTotalSettledAmount(groupId);
    }

    @GetMapping("/pending/count")
    @Operation(summary = "Get count of pending settlements for the current user")
    public Mono<Long> getPendingCount(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return settlementService.countPendingSettlements(userId);
    }
}
