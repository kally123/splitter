package com.splitter.expense.controller;

import com.splitter.expense.dto.*;
import com.splitter.expense.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * REST controller for expense operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
@Tag(name = "Expenses", description = "Expense management operations")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new expense")
    public Mono<ExpenseDto> createExpense(
            @Valid @RequestBody CreateExpenseRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("Creating expense '{}' by user {}", request.description(), userId);
        return expenseService.createExpense(request, userId);
    }

    @GetMapping("/{expenseId}")
    @Operation(summary = "Get expense by ID")
    public Mono<ExpenseDto> getExpense(@PathVariable UUID expenseId) {
        return expenseService.getExpenseById(expenseId);
    }

    @GetMapping
    @Operation(summary = "Get expenses for a group with pagination")
    public Flux<ExpenseDto> getGroupExpenses(
            @RequestParam UUID groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return expenseService.getGroupExpenses(groupId, page, Math.min(size, 100));
    }

    @GetMapping("/group/{groupId}/all")
    @Operation(summary = "Get all expenses for a group")
    public Flux<ExpenseDto> getAllGroupExpenses(@PathVariable UUID groupId) {
        return expenseService.getAllGroupExpenses(groupId);
    }

    @GetMapping("/group/{groupId}/range")
    @Operation(summary = "Get expenses by date range")
    public Flux<ExpenseDto> getExpensesByDateRange(
            @PathVariable UUID groupId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return expenseService.getExpensesByDateRange(groupId, startDate, endDate);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent expenses for the current user")
    public Flux<ExpenseDto> getRecentExpenses(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "10") int limit) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return expenseService.getRecentExpensesForUser(userId, Math.min(limit, 50));
    }

    @PutMapping("/{expenseId}")
    @Operation(summary = "Update an expense")
    public Mono<ExpenseDto> updateExpense(
            @PathVariable UUID expenseId,
            @Valid @RequestBody UpdateExpenseRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("Updating expense {} by user {}", expenseId, userId);
        return expenseService.updateExpense(expenseId, request, userId);
    }

    @DeleteMapping("/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an expense")
    public Mono<Void> deleteExpense(
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("Deleting expense {} by user {}", expenseId, userId);
        return expenseService.deleteExpense(expenseId, userId);
    }

    @GetMapping("/group/{groupId}/count")
    @Operation(summary = "Get expense count for a group")
    public Mono<Long> getExpenseCount(@PathVariable UUID groupId) {
        return expenseService.getExpenseCount(groupId);
    }

    @GetMapping("/group/{groupId}/total")
    @Operation(summary = "Get total expense amount for a group")
    public Mono<BigDecimal> getTotalAmount(@PathVariable UUID groupId) {
        return expenseService.getTotalExpenseAmount(groupId);
    }
}
