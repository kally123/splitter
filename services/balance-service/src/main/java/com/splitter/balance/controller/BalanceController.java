package com.splitter.balance.controller;

import com.splitter.balance.dto.BalanceDto;
import com.splitter.balance.dto.GroupBalanceSummary;
import com.splitter.balance.service.BalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST controller for balance operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/balances")
@RequiredArgsConstructor
@Tag(name = "Balances", description = "Balance and debt management operations")
public class BalanceController {

    private final BalanceService balanceService;

    @GetMapping("/group/{groupId}")
    @Operation(summary = "Get all balances for a group")
    public Flux<BalanceDto> getGroupBalances(@PathVariable UUID groupId) {
        return balanceService.getGroupBalances(groupId);
    }

    @GetMapping("/group/{groupId}/active")
    @Operation(summary = "Get active (non-zero) debts for a group")
    public Flux<BalanceDto> getActiveDebts(@PathVariable UUID groupId) {
        return balanceService.getActiveDebts(groupId);
    }

    @GetMapping("/group/{groupId}/summary")
    @Operation(summary = "Get balance summary for a group with simplified debts")
    public Mono<GroupBalanceSummary> getGroupBalanceSummary(@PathVariable UUID groupId) {
        return balanceService.getGroupBalanceSummary(groupId);
    }

    @GetMapping("/group/{groupId}/between")
    @Operation(summary = "Get balance between two users in a group")
    public Mono<BalanceDto> getBalanceBetween(
            @PathVariable UUID groupId,
            @RequestParam UUID userId1,
            @RequestParam UUID userId2) {
        return balanceService.getBalanceBetween(groupId, userId1, userId2);
    }

    @GetMapping("/user")
    @Operation(summary = "Get all balances for the current user")
    public Flux<BalanceDto> getCurrentUserBalances(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return balanceService.getUserBalances(userId);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all balances for a specific user")
    public Flux<BalanceDto> getUserBalances(@PathVariable UUID userId) {
        return balanceService.getUserBalances(userId);
    }
}
