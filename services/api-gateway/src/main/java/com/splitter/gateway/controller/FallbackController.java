package com.splitter.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Fallback controller for when downstream services are unavailable.
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping(value = "/{serviceName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> serviceFallback(@PathVariable String serviceName) {
        log.warn("Fallback triggered for service: {}", serviceName);

        return Mono.just(Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "error", "Service Unavailable",
                "message", String.format("The %s is currently unavailable. Please try again later.", serviceName),
                "service", serviceName
        ));
    }

    @GetMapping(value = "/default", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> defaultFallback() {
        log.warn("Default fallback triggered");

        return Mono.just(Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "error", "Service Unavailable",
                "message", "The service is currently unavailable. Please try again later."
        ));
    }
}
