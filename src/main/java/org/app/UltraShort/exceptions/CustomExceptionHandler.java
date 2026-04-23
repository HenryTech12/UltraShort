package org.app.UltraShort.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class CustomExceptionHandler {


    // Helper to keep code DRY (Don't Repeat Yourself)
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String error, String message, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", System.currentTimeMillis());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("path", path);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(URLNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUrlNotFoundException(URLNotFoundException urlNotFoundException, HttpServletRequest request, HttpServletResponse response) {
        return buildResponse(HttpStatus.NOT_FOUND,"Not Found", "URL not found",request.getRequestURI());
    }

    @ExceptionHandler(RetryException.class)
    public ResponseEntity<Map<String, Object>> handleRetryException(RetryException retryException, HttpServletRequest request, HttpServletResponse response) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,"Retry failed!!!", "Server is down, retry mechanism is failing",request.getRequestURI());

    }

    @ExceptionHandler(ServerFailedException.class)
    public ResponseEntity<Map<String, Object>> handleServerDownException(ServerFailedException serverFailedException, HttpServletRequest request, HttpServletResponse response) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,"Server is down!!!", "Server is down",request.getRequestURI());
    }

    @ExceptionHandler(ServerManyRequestException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimiterException(ServerManyRequestException manyRequestException, HttpServletRequest request, HttpServletResponse response) {
        return buildResponse(HttpStatus.NOT_FOUND,"Too many Request !!!", "Request rejected, Too many request",request.getRequestURI());
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolationException(DataIntegrityViolationException exception, HttpServletRequest request, HttpServletResponse response) {
        String message = "The URL provided is too long or contains invalid data.";

        // We check if it's specifically a 'Data too long' error
        if (exception.getMessage() != null && exception.getMessage().contains("Data too long")) {
            message = "URL exceeds the maximum allowed length (2048 characters).";
        }
        return buildResponse(HttpStatus.BAD_REQUEST ,"An error occurred", message,request.getRequestURI());
    }
}
