package com.splitter.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Standard error response format for all API errors.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * Timestamp when the error occurred.
     */
    private Instant timestamp;

    /**
     * HTTP status code.
     */
    private int status;

    /**
     * Error type/category.
     */
    private String error;

    /**
     * Human-readable error message.
     */
    private String message;

    /**
     * Request path that caused the error.
     */
    private String path;

    /**
     * Trace ID for debugging (correlation ID).
     */
    private String traceId;

    /**
     * List of field-level validation errors.
     */
    private List<FieldError> fieldErrors;

    /**
     * Represents a field-level validation error.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }

    /**
     * Creates a simple error response.
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .build();
    }

    /**
     * Creates a validation error response.
     */
    public static ErrorResponse validation(String path, List<FieldError> fieldErrors) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(400)
                .error("Validation Error")
                .message("Request validation failed")
                .path(path)
                .fieldErrors(fieldErrors)
                .build();
    }
}
