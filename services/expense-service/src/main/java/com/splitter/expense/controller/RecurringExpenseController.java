package com.splitter.expense.controller;

import com.splitter.expense.dto.CreateRecurringExpenseRequest;
import com.splitter.expense.dto.RecurringExpenseResponse;
import com.splitter.expense.dto.UpdateRecurringExpenseRequest;
import com.splitter.expense.scheduler.RecurringExpenseScheduler;
import com.splitter.expense.service.RecurringExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recurring-expenses")
@RequiredArgsConstructor
public class RecurringExpenseController {

    private final RecurringExpenseService recurringExpenseService;
    private final RecurringExpenseScheduler scheduler;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RecurringExpenseResponse> create(
            @Valid @RequestBody CreateRecurringExpenseRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return recurringExpenseService.create(request, userId);
    }

    @GetMapping
    public Flux<RecurringExpenseResponse> getByGroup(
            @RequestParam UUID groupId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return recurringExpenseService.getByGroup(groupId, userId);
    }

    @GetMapping("/my")
    public Flux<RecurringExpenseResponse> getMyRecurringExpenses(
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return recurringExpenseService.getByUser(userId);
    }

    @GetMapping("/{id}")
    public Mono<RecurringExpenseResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return recurringExpenseService.getById(id, userId);
    }

    @PutMapping("/{id}")
    public Mono<RecurringExpenseResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRecurringExpenseRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return recurringExpenseService.update(id, request, userId);
    }

    @PostMapping("/{id}/pause")
    public Mono<RecurringExpenseResponse> pause(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return recurringExpenseService.pause(id, userId);
    }

    @PostMapping("/{id}/resume")
    public Mono<RecurringExpenseResponse> resume(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return recurringExpenseService.resume(id, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return recurringExpenseService.delete(id, userId);
    }

    @PostMapping("/process")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Long> triggerProcessing(@AuthenticationPrincipal Jwt jwt) {
        // Admin-only endpoint to manually trigger processing
        return scheduler.processNow();
    }
}
