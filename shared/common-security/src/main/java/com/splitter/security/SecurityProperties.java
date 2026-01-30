package com.splitter.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * Security configuration properties for the Splitter platform.
 * Externalized configuration for production deployments.
 */
@ConfigurationProperties(prefix = "splitter.security")
@Validated
public class SecurityProperties {

    @NotNull
    private JwtProperties jwt = new JwtProperties();

    @NotNull
    private CorsProperties cors = new CorsProperties();

    @NotNull
    private RateLimitProperties rateLimit = new RateLimitProperties();

    @NotNull
    private PasswordProperties password = new PasswordProperties();

    // Getters and Setters
    public JwtProperties getJwt() { return jwt; }
    public void setJwt(JwtProperties jwt) { this.jwt = jwt; }

    public CorsProperties getCors() { return cors; }
    public void setCors(CorsProperties cors) { this.cors = cors; }

    public RateLimitProperties getRateLimit() { return rateLimit; }
    public void setRateLimit(RateLimitProperties rateLimit) { this.rateLimit = rateLimit; }

    public PasswordProperties getPassword() { return password; }
    public void setPassword(PasswordProperties password) { this.password = password; }

    /**
     * JWT configuration properties.
     */
    public static class JwtProperties {
        @NotEmpty
        private String secret;

        private Duration accessTokenExpiration = Duration.ofMinutes(15);

        private Duration refreshTokenExpiration = Duration.ofDays(7);

        private String issuer = "splitter";

        private String audience = "splitter-api";

        // Getters and Setters
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }

        public Duration getAccessTokenExpiration() { return accessTokenExpiration; }
        public void setAccessTokenExpiration(Duration accessTokenExpiration) { 
            this.accessTokenExpiration = accessTokenExpiration; 
        }

        public Duration getRefreshTokenExpiration() { return refreshTokenExpiration; }
        public void setRefreshTokenExpiration(Duration refreshTokenExpiration) { 
            this.refreshTokenExpiration = refreshTokenExpiration; 
        }

        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }

        public String getAudience() { return audience; }
        public void setAudience(String audience) { this.audience = audience; }
    }

    /**
     * CORS configuration properties.
     */
    public static class CorsProperties {
        @NotEmpty
        private List<String> allowedOrigins = List.of("http://localhost:3000");

        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

        private List<String> allowedHeaders = List.of("*");

        private List<String> exposedHeaders = List.of("X-Request-ID", "X-RateLimit-Remaining");

        private boolean allowCredentials = true;

        private Duration maxAge = Duration.ofHours(1);

        // Getters and Setters
        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }

        public List<String> getAllowedMethods() { return allowedMethods; }
        public void setAllowedMethods(List<String> allowedMethods) { this.allowedMethods = allowedMethods; }

        public List<String> getAllowedHeaders() { return allowedHeaders; }
        public void setAllowedHeaders(List<String> allowedHeaders) { this.allowedHeaders = allowedHeaders; }

        public List<String> getExposedHeaders() { return exposedHeaders; }
        public void setExposedHeaders(List<String> exposedHeaders) { this.exposedHeaders = exposedHeaders; }

        public boolean isAllowCredentials() { return allowCredentials; }
        public void setAllowCredentials(boolean allowCredentials) { this.allowCredentials = allowCredentials; }

        public Duration getMaxAge() { return maxAge; }
        public void setMaxAge(Duration maxAge) { this.maxAge = maxAge; }
    }

    /**
     * Rate limiting configuration properties.
     */
    public static class RateLimitProperties {
        @Min(1)
        private int requestsPerMinute = 60;

        @Min(1)
        private int authRequestsPerMinute = 10;

        @Min(1)
        private int burstCapacity = 10;

        private boolean enabled = true;

        // Getters and Setters
        public int getRequestsPerMinute() { return requestsPerMinute; }
        public void setRequestsPerMinute(int requestsPerMinute) { this.requestsPerMinute = requestsPerMinute; }

        public int getAuthRequestsPerMinute() { return authRequestsPerMinute; }
        public void setAuthRequestsPerMinute(int authRequestsPerMinute) { 
            this.authRequestsPerMinute = authRequestsPerMinute; 
        }

        public int getBurstCapacity() { return burstCapacity; }
        public void setBurstCapacity(int burstCapacity) { this.burstCapacity = burstCapacity; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    /**
     * Password policy configuration.
     */
    public static class PasswordProperties {
        @Min(8)
        private int minLength = 8;

        @Min(1)
        private int minUppercase = 1;

        @Min(1)
        private int minLowercase = 1;

        @Min(1)
        private int minDigits = 1;

        @Min(0)
        private int minSpecialChars = 1;

        @Min(1)
        private int maxFailedAttempts = 5;

        private Duration lockoutDuration = Duration.ofMinutes(15);

        @Min(4)
        private int bcryptStrength = 12;

        // Getters and Setters
        public int getMinLength() { return minLength; }
        public void setMinLength(int minLength) { this.minLength = minLength; }

        public int getMinUppercase() { return minUppercase; }
        public void setMinUppercase(int minUppercase) { this.minUppercase = minUppercase; }

        public int getMinLowercase() { return minLowercase; }
        public void setMinLowercase(int minLowercase) { this.minLowercase = minLowercase; }

        public int getMinDigits() { return minDigits; }
        public void setMinDigits(int minDigits) { this.minDigits = minDigits; }

        public int getMinSpecialChars() { return minSpecialChars; }
        public void setMinSpecialChars(int minSpecialChars) { this.minSpecialChars = minSpecialChars; }

        public int getMaxFailedAttempts() { return maxFailedAttempts; }
        public void setMaxFailedAttempts(int maxFailedAttempts) { this.maxFailedAttempts = maxFailedAttempts; }

        public Duration getLockoutDuration() { return lockoutDuration; }
        public void setLockoutDuration(Duration lockoutDuration) { this.lockoutDuration = lockoutDuration; }

        public int getBcryptStrength() { return bcryptStrength; }
        public void setBcryptStrength(int bcryptStrength) { this.bcryptStrength = bcryptStrength; }
    }
}
