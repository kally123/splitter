package com.splitter.common.observability;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for distributed tracing using OpenTelemetry.
 * Exports traces to Jaeger/OTLP collector for visualization.
 */
@Configuration
public class TracingConfig {

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${otel.exporter.otlp.endpoint:http://localhost:4317}")
    private String otlpEndpoint;

    @Value("${otel.traces.sampler.probability:1.0}")
    private double samplingProbability;

    @Bean
    public OpenTelemetry openTelemetry() {
        Resource resource = Resource.getDefault()
            .merge(Resource.builder()
                .put(ResourceAttributes.SERVICE_NAME, serviceName)
                .put(ResourceAttributes.SERVICE_VERSION, "1.0.0")
                .put(ResourceAttributes.DEPLOYMENT_ENVIRONMENT, 
                    System.getenv().getOrDefault("ENVIRONMENT", "development"))
                .build());

        OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
            .setEndpoint(otlpEndpoint)
            .setTimeout(Duration.ofSeconds(10))
            .build();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(spanExporter)
                .setMaxQueueSize(2048)
                .setMaxExportBatchSize(512)
                .setScheduleDelay(Duration.ofSeconds(1))
                .build())
            .setSampler(Sampler.traceIdRatioBased(samplingProbability))
            .setResource(resource)
            .build();

        return OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setPropagators(ContextPropagators.create(
                io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator.getInstance()))
            .buildAndRegisterGlobal();
    }

    /**
     * Web filter to add trace context to requests and propagate headers.
     */
    @Bean
    public WebFilter tracingWebFilter(Tracer tracer) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // Extract or generate trace context headers
            String traceId = request.getHeaders().getFirst("traceparent");
            String correlationId = request.getHeaders().getFirst("X-Correlation-ID");
            
            if (correlationId == null) {
                correlationId = java.util.UUID.randomUUID().toString();
            }
            
            // Add trace headers to response
            ServerHttpResponse response = exchange.getResponse();
            response.getHeaders().add("X-Correlation-ID", correlationId);
            
            if (tracer.currentSpan() != null) {
                response.getHeaders().add("X-Trace-ID", 
                    tracer.currentSpan().context().traceId());
            }
            
            // Continue chain with modified exchange
            String finalCorrelationId = correlationId;
            return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put("correlationId", finalCorrelationId));
        };
    }
}
