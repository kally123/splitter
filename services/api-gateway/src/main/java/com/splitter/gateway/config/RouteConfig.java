package com.splitter.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Route configuration for the API Gateway.
 * Defines how requests are routed to backend services.
 */
@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // User Service Routes
                .route("user-service", r -> r
                        .path("/api/v1/users/**", "/api/v1/auth/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c
                                        .setName("userServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/user-service"))
                                .retry(retryConfig -> retryConfig
                                        .setRetries(3)
                                        .setStatuses(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)))
                        .uri("lb://user-service"))

                // Group Service Routes
                .route("group-service", r -> r
                        .path("/api/v1/groups/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c
                                        .setName("groupServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/group-service")))
                        .uri("lb://group-service"))

                // Expense Service Routes
                .route("expense-service", r -> r
                        .path("/api/v1/expenses/**", "/api/v1/categories/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c
                                        .setName("expenseServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/expense-service")))
                        .uri("lb://expense-service"))

                // Balance Service Routes
                .route("balance-service", r -> r
                        .path("/api/v1/balances/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c
                                        .setName("balanceServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/balance-service")))
                        .uri("lb://balance-service"))

                // Settlement Service Routes
                .route("settlement-service", r -> r
                        .path("/api/v1/settlements/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c
                                        .setName("settlementServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/settlement-service")))
                        .uri("lb://settlement-service"))

                // Notification Service Routes
                .route("notification-service", r -> r
                        .path("/api/v1/notifications/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c
                                        .setName("notificationServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/notification-service")))
                        .uri("lb://notification-service"))

                .build();
    }
}
