package com.splitter.payment.controller;

import com.splitter.payment.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "Payment provider webhook handlers")
public class WebhookController {
    
    private final WebhookService webhookService;
    
    @PostMapping("/stripe")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Handle Stripe webhook events")
    public Mono<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        log.info("Received Stripe webhook");
        return webhookService.handleStripeWebhook(payload, signature)
            .thenReturn("OK");
    }
}
