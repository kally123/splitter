package com.splitter.common.security;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Instant;
import java.util.*;

/**
 * Aspect for comprehensive audit logging of security-relevant operations.
 * Logs are written to a dedicated audit logger in structured JSON format.
 */
@Aspect
@Component
public class AuditLogAspect {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");
    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);
    
    private final ObjectMapper objectMapper;

    public AuditLogAspect() {
        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * Audit successful operations marked with @Auditable
     */
    @AfterReturning(
        pointcut = "@annotation(auditable)",
        returning = "result"
    )
    public void auditSuccess(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            AuditEvent event = buildAuditEvent(joinPoint, auditable, true, null);
            event.setResourceId(extractResourceId(joinPoint, auditable, result));
            
            writeAuditLog(event);
        } catch (Exception e) {
            log.error("Failed to write audit log", e);
        }
    }

    /**
     * Audit failed operations marked with @Auditable
     */
    @AfterThrowing(
        pointcut = "@annotation(auditable)",
        throwing = "exception"
    )
    public void auditFailure(JoinPoint joinPoint, Auditable auditable, Throwable exception) {
        try {
            AuditEvent event = buildAuditEvent(joinPoint, auditable, false, exception.getMessage());
            writeAuditLog(event);
        } catch (Exception e) {
            log.error("Failed to write audit log for failure", e);
        }
    }

    /**
     * Audit all authentication attempts
     */
    @Around("execution(* com.splitter..*.AuthController.login(..))")
    public Object auditLogin(ProceedingJoinPoint joinPoint) throws Throwable {
        String email = extractLoginEmail(joinPoint);
        Instant startTime = Instant.now();
        
        try {
            Object result = joinPoint.proceed();
            
            // Handle reactive result
            if (result instanceof Mono<?>) {
                return ((Mono<?>) result)
                    .doOnSuccess(r -> logAuthEvent("LOGIN", email, true, null))
                    .doOnError(e -> logAuthEvent("LOGIN", email, false, e.getMessage()));
            }
            
            logAuthEvent("LOGIN", email, true, null);
            return result;
        } catch (Exception e) {
            logAuthEvent("LOGIN", email, false, e.getMessage());
            throw e;
        }
    }

    /**
     * Audit logout events
     */
    @AfterReturning("execution(* com.splitter..*.AuthController.logout(..))")
    public void auditLogout(JoinPoint joinPoint) {
        logAuthEvent("LOGOUT", getCurrentUserId(), true, null);
    }

    /**
     * Audit password changes
     */
    @AfterReturning("execution(* com.splitter..*.UserController.changePassword(..))")
    public void auditPasswordChange(JoinPoint joinPoint) {
        AuditEvent event = AuditEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .timestamp(Instant.now())
            .action("PASSWORD_CHANGE")
            .resourceType("USER")
            .userId(getCurrentUserId())
            .success(true)
            .build();
        
        writeAuditLog(event);
    }

    /**
     * Audit admin operations
     */
    @Around("@annotation(org.springframework.security.access.prepost.PreAuthorize) && " +
            "execution(* com.splitter..*.Admin*.*(..))")
    public Object auditAdminOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Instant startTime = Instant.now();
        
        try {
            Object result = joinPoint.proceed();
            
            AuditEvent event = AuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(startTime)
                .action("ADMIN_" + methodName.toUpperCase())
                .resourceType("SYSTEM")
                .userId(getCurrentUserId())
                .success(true)
                .metadata(Map.of("method", methodName))
                .build();
            
            writeAuditLog(event);
            return result;
        } catch (Exception e) {
            AuditEvent event = AuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(startTime)
                .action("ADMIN_" + methodName.toUpperCase())
                .resourceType("SYSTEM")
                .userId(getCurrentUserId())
                .success(false)
                .errorMessage(e.getMessage())
                .build();
            
            writeAuditLog(event);
            throw e;
        }
    }

    private AuditEvent buildAuditEvent(JoinPoint joinPoint, Auditable auditable, 
                                        boolean success, String errorMessage) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        
        return AuditEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .timestamp(Instant.now())
            .action(auditable.action())
            .resourceType(auditable.resourceType())
            .userId(getCurrentUserId())
            .clientIp(getClientIp())
            .userAgent(getUserAgent())
            .correlationId(getCorrelationId())
            .success(success)
            .errorMessage(errorMessage)
            .metadata(extractRelevantParams(joinPoint, signature))
            .build();
    }

    private String extractResourceId(JoinPoint joinPoint, Auditable auditable, Object result) {
        // Try to get from specified parameter
        if (!auditable.resourceIdParam().isEmpty()) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            
            for (int i = 0; i < paramNames.length; i++) {
                if (paramNames[i].equals(auditable.resourceIdParam())) {
                    return args[i] != null ? args[i].toString() : null;
                }
            }
        }
        
        // Try to extract from result object
        if (result != null) {
            try {
                Method getId = result.getClass().getMethod("getId");
                Object id = getId.invoke(result);
                return id != null ? id.toString() : null;
            } catch (Exception ignored) {}
        }
        
        return null;
    }

    private Map<String, Object> extractRelevantParams(JoinPoint joinPoint, MethodSignature signature) {
        Map<String, Object> params = new HashMap<>();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        
        for (int i = 0; i < paramNames.length; i++) {
            // Skip sensitive or large parameters
            if (isSensitiveParam(paramNames[i]) || args[i] instanceof ServerWebExchange) {
                continue;
            }
            
            // Only include simple types
            if (args[i] == null || isSimpleType(args[i].getClass())) {
                params.put(paramNames[i], args[i]);
            } else {
                params.put(paramNames[i], args[i].getClass().getSimpleName());
            }
        }
        
        return params;
    }

    private boolean isSensitiveParam(String paramName) {
        return paramName.toLowerCase().contains("password") ||
               paramName.toLowerCase().contains("secret") ||
               paramName.toLowerCase().contains("token") ||
               paramName.toLowerCase().contains("credential");
    }

    private boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive() ||
               clazz == String.class ||
               clazz == UUID.class ||
               Number.class.isAssignableFrom(clazz) ||
               clazz == Boolean.class;
    }

    private void logAuthEvent(String action, String identifier, boolean success, String error) {
        AuditEvent event = AuditEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .timestamp(Instant.now())
            .action(action)
            .resourceType("AUTHENTICATION")
            .userId(identifier)
            .clientIp(getClientIp())
            .userAgent(getUserAgent())
            .success(success)
            .errorMessage(error)
            .build();
        
        writeAuditLog(event);
    }

    private String extractLoginEmail(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg != null) {
                try {
                    Method getEmail = arg.getClass().getMethod("getEmail");
                    Object email = getEmail.invoke(arg);
                    return email != null ? email.toString() : "unknown";
                } catch (Exception ignored) {}
            }
        }
        return "unknown";
    }

    private String getCurrentUserId() {
        return MDC.get("userId") != null ? MDC.get("userId") : "anonymous";
    }

    private String getClientIp() {
        return MDC.get("clientIp") != null ? MDC.get("clientIp") : "unknown";
    }

    private String getUserAgent() {
        return MDC.get("userAgent") != null ? MDC.get("userAgent") : "unknown";
    }

    private String getCorrelationId() {
        return MDC.get("correlationId") != null ? MDC.get("correlationId") : UUID.randomUUID().toString();
    }

    private void writeAuditLog(AuditEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            auditLog.info(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit event", e);
            auditLog.info("action={} resourceType={} userId={} success={}", 
                event.getAction(), event.getResourceType(), event.getUserId(), event.isSuccess());
        }
    }

    /**
     * Audit event data structure
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AuditEvent {
        private String eventId;
        private Instant timestamp;
        private String action;
        private String resourceType;
        private String resourceId;
        private String userId;
        private String clientIp;
        private String userAgent;
        private String correlationId;
        private boolean success;
        private String errorMessage;
        private Map<String, Object> metadata;
    }
}
