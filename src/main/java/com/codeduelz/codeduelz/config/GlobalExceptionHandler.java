package com.codeduelz.codeduelz.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Global exception handler — ensures all unhandled exceptions return
 * a clean JSON response instead of Spring's default HTML error page.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.warn("Request failed: {}", ex.getMessage());

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = ex.getMessage();

        // Map common exception messages to proper HTTP status codes
        if (message != null) {
            String lower = message.toLowerCase();
            if (lower.contains("not found")) {
                status = HttpStatus.NOT_FOUND;
            } else if (lower.contains("already") || lower.contains("duplicate")) {
                status = HttpStatus.CONFLICT;
            } else if (lower.contains("invalid") || lower.contains("bad")) {
                status = HttpStatus.BAD_REQUEST;
            } else if (lower.contains("unauthorized") || lower.contains("token")) {
                status = HttpStatus.UNAUTHORIZED;
            }
        }

        return ResponseEntity.status(status).body(Map.of(
                "error", status.getReasonPhrase(),
                "message", message != null ? message : "Unknown error",
                "status", status.value(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal Server Error",
                "message", "An unexpected error occurred",
                "status", 500,
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
