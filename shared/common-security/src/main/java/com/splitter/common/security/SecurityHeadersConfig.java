package com.splitter.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;
import org.springframework.security.web.server.header.XXssProtectionServerHttpHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Security configuration with comprehensive security headers.
 * Implements OWASP security header recommendations.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityHeadersConfig {

    private final JwtAuthenticationManager authenticationManager;
    private final ServerSecurityContextRepository securityContextRepository;

    public SecurityHeadersConfig(
            JwtAuthenticationManager authenticationManager,
            ServerSecurityContextRepository securityContextRepository) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            // Disable CSRF for stateless API
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Authentication
            .authenticationManager(authenticationManager)
            .securityContextRepository(securityContextRepository)
            
            // Security Headers
            .headers(headers -> headers
                // X-Frame-Options: DENY - Prevent clickjacking
                .frameOptions(frame -> frame
                    .mode(XFrameOptionsServerHttpHeadersWriter.Mode.DENY))
                
                // X-Content-Type-Options: nosniff - Prevent MIME sniffing
                .contentTypeOptions(contentType -> {})
                
                // X-XSS-Protection: 1; mode=block - XSS filter
                .xssProtection(xss -> xss
                    .headerValue(XXssProtectionServerHttpHeadersWriter.HeaderValue.ENABLED_MODE_BLOCK))
                
                // Strict-Transport-Security - Force HTTPS
                .hsts(hsts -> hsts
                    .includeSubdomains(true)
                    .maxAge(Duration.ofDays(365))
                    .preload(true))
                
                // Content-Security-Policy - Control resource loading
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives(buildCspPolicy()))
                
                // Referrer-Policy - Control referrer information
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                
                // Permissions-Policy - Restrict browser features
                .permissionsPolicy(permissions -> permissions
                    .policy(buildPermissionsPolicy()))
                
                // Cache-Control for API responses
                .cache(cache -> {})
            )
            
            // Authorization rules
            .authorizeExchange(exchanges -> exchanges
                // Public endpoints
                .pathMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .pathMatchers("/actuator/info").permitAll()
                .pathMatchers("/actuator/prometheus").permitAll()
                .pathMatchers("/api/v1/auth/**").permitAll()
                .pathMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Webhook endpoints (verified by signature)
                .pathMatchers("/api/v1/webhooks/**").permitAll()
                
                // All other endpoints require authentication
                .anyExchange().authenticated()
            )
            
            .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allowed origins (configure per environment)
        configuration.setAllowedOrigins(List.of(
            "http://localhost:3000",
            "http://localhost:5173",
            "https://app.splitter.com",
            "https://staging.splitter.com"
        ));
        
        // Allowed methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        
        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With",
            "X-Correlation-ID",
            "X-Request-ID"
        ));
        
        // Exposed headers
        configuration.setExposedHeaders(Arrays.asList(
            "X-Correlation-ID",
            "X-Request-ID",
            "X-RateLimit-Limit",
            "X-RateLimit-Remaining",
            "X-RateLimit-Reset",
            "ETag",
            "Last-Modified"
        ));
        
        // Allow credentials
        configuration.setAllowCredentials(true);
        
        // Cache preflight response
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private String buildCspPolicy() {
        return String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data: https:",
            "font-src 'self' https://fonts.gstatic.com",
            "connect-src 'self' https://api.stripe.com wss:",
            "frame-src https://js.stripe.com https://hooks.stripe.com",
            "frame-ancestors 'none'",
            "form-action 'self'",
            "base-uri 'self'",
            "object-src 'none'",
            "upgrade-insecure-requests"
        );
    }

    private String buildPermissionsPolicy() {
        return String.join(", ",
            "camera=(self)",
            "microphone=()",
            "geolocation=(self)",
            "payment=(self)",
            "usb=()",
            "magnetometer=()",
            "gyroscope=()",
            "accelerometer=()"
        );
    }
}
