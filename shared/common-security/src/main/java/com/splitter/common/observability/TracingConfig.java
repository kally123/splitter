package com.splitter.common.observability;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.WebFilter;

import java.util.UUID;

/**
 * Configuration for distributed tracing.
 * Adds correlation ID to requests and responses for traceability.
 */
@Configuration
@ConditionalOnProperty(name = "splitter.observability.tracing.enabled", havingValue = "true", matchIfMissing = true)
public class TracingConfig {

    @Value("${spring.application.name:splitter-service}")
    private String serviceName;

    /**
     * Web filter to add trace context to requests and propagate headers.
     */
    @Bean
    public WebFilter tracingWebFilter() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // Extract or generate correlation ID
            String correlationId = request.getHeaders().getFirst("X-Correlation-ID");
            
            if (correlationId == null) {
                correlationId = UUID.randomUUID().toString();
            }
            
            // Add trace headers to response
            ServerHttpResponse response = exchange.getResponse();
            response.getHeaders().add("X-Correlation-ID", correlationId);
            response.getHeaders().add("X-Service-Name", serviceName);
            
            // Continue chain with modified exchange
            String finalCorrelationId = correlationId;
            return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put("correlationId", finalCorrelationId));
        };
    }
}
