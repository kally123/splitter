package com.splitter.payment.controller;

import com.splitter.payment.dto.*;
import com.splitter.payment.service.PaymentService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Payment processing API")
public class PaymentController {
    
    private final PaymentService paymentService;
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new payment")
    public Mono<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} creating payment from {} to {}", userId, request.getFromUserId(), request.getToUserId());
        return paymentService.createPayment(request, userId);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    public Mono<PaymentResponse> getPayment(@PathVariable UUID id) {
        return paymentService.getPayment(id);
    }
    
    @GetMapping
    @Operation(summary = "Get all payments for current user")
    public Flux<PaymentResponse> getUserPayments(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return paymentService.getUserPayments(userId);
    }
    
    @GetMapping("/group/{groupId}")
    @Operation(summary = "Get all payments for a group")
    public Flux<PaymentResponse> getGroupPayments(@PathVariable UUID groupId) {
        return paymentService.getGroupPayments(groupId);
    }
    
    @PostMapping("/{id}/refund")
    @Operation(summary = "Refund a payment")
    public Mono<PaymentResponse> refundPayment(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} requesting refund for payment {}", userId, id);
        return paymentService.refundPayment(id, userId);
    }
    
    // Payment Methods
    
    @GetMapping("/methods")
    @Operation(summary = "Get user's payment methods")
    public Flux<PaymentMethodResponse> getPaymentMethods(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return paymentService.getUserPaymentMethods(userId);
    }
    
    @PostMapping("/methods")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a new payment method")
    public Mono<PaymentMethodResponse> addPaymentMethod(
            @Valid @RequestBody AddPaymentMethodRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} adding payment method", userId);
        return paymentService.addPaymentMethod(request, userId);
    }
    
    @DeleteMapping("/methods/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a payment method")
    public Mono<Void> removePaymentMethod(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return paymentService.removePaymentMethod(id, userId);
    }
    
    @PutMapping("/methods/{id}/default")
    @Operation(summary = "Set payment method as default")
    public Mono<PaymentMethodResponse> setDefaultPaymentMethod(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return paymentService.setDefaultPaymentMethod(id, userId);
    }
}
