package com.splitter.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Production-ready security configuration with hardened settings.
 * This configuration implements security best practices for the Splitter platform.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class ProductionSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityProperties securityProperties;

    public ProductionSecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            SecurityProperties securityProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.securityProperties = securityProperties;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            // Disable CSRF for stateless API (using JWT)
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Disable form login and HTTP basic (API only)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            
            // Stateless session management
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            
            // Configure security headers
            .headers(headers -> headers
                // Prevent clickjacking
                .frameOptions(frameOptions -> 
                    frameOptions.mode(XFrameOptionsServerHttpHeadersWriter.Mode.DENY))
                
                // Content Security Policy
                .contentSecurityPolicy(csp -> 
                    csp.policyDirectives(buildContentSecurityPolicy()))
                
                // Referrer Policy
                .referrerPolicy(referrer -> 
                    referrer.policy(ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                
                // Permissions Policy (Feature Policy replacement)
                .permissionsPolicy(permissions -> 
                    permissions.policy("geolocation=(), microphone=(), camera=()"))
                
                // HSTS - Strict Transport Security
                .hsts(hsts -> hsts
                    .includeSubdomains(true)
                    .preload(true)
                    .maxAge(Duration.ofDays(365)))
                
                // Prevent MIME type sniffing
                .contentTypeOptions(contentTypeOptions -> {})
                
                // XSS Protection (legacy, but still useful for older browsers)
                .xssProtection(xss -> xss.headerValue(
                    org.springframework.security.web.server.header.XXssProtectionServerHttpHeadersWriter.HeaderValue.ENABLED_MODE_BLOCK))
            )
            
            // Configure authorization rules
            .authorizeExchange(exchanges -> exchanges
                // Public endpoints
                .pathMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/register").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                .pathMatchers(HttpMethod.GET, "/api/v1/auth/verify-email").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/v1/auth/forgot-password").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/v1/auth/reset-password").permitAll()
                
                // Health and metrics endpoints
                .pathMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .pathMatchers("/actuator/info").permitAll()
                .pathMatchers("/actuator/prometheus").permitAll()
                
                // OpenAPI documentation
                .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                
                // WebSocket endpoint (authentication handled separately)
                .pathMatchers("/ws/**").permitAll()
                
                // Admin endpoints require admin role
                .pathMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .pathMatchers("/actuator/**").hasRole("ADMIN")
                
                // All other endpoints require authentication
                .anyExchange().authenticated()
            )
            
            // Add JWT authentication filter
            .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            
            // Exception handling
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint((exchange, ex) -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return Mono.empty();
                })
                .accessDeniedHandler((exchange, denied) -> {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return Mono.empty();
                })
            )
            
            .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allowed origins from configuration
        configuration.setAllowedOrigins(securityProperties.getCors().getAllowedOrigins());
        
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
            "X-Request-ID"
        ));
        
        // Exposed headers
        configuration.setExposedHeaders(Arrays.asList(
            "X-Request-ID",
            "X-RateLimit-Limit",
            "X-RateLimit-Remaining",
            "X-RateLimit-Reset"
        ));
        
        // Allow credentials (for cookies if needed)
        configuration.setAllowCredentials(true);
        
        // Cache preflight response
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt with strength 12 (2^12 iterations)
        // Balance between security and performance
        return new BCryptPasswordEncoder(12);
    }

    private String buildContentSecurityPolicy() {
        return String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data: https:",
            "font-src 'self'",
            "connect-src 'self' " + String.join(" ", securityProperties.getCors().getAllowedOrigins()),
            "frame-ancestors 'none'",
            "form-action 'self'",
            "base-uri 'self'",
            "object-src 'none'"
        );
    }
}
