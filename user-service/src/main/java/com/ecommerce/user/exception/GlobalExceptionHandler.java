package com.ecommerce.user.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler for User Service.
 * This class provides centralized exception handling across the entire User Service.
 * It catches and handles various types of exceptions, converting them into
 * appropriate HTTP responses with meaningful error messages.
 * 
 * Key Features:
 * - Centralized error handling
 * - Consistent error response format
 * - Proper HTTP status codes
 * - Logging for debugging and monitoring
 * - Security-conscious error messages
 * 
 * @author Ecommerce Development Team
 * @version 1.0
 * @since 2024-01-01
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handles custom UserServiceException instances.
     * These are business logic exceptions specific to user operations.
     * 
     * @param ex The UserServiceException that was thrown
     * @param request The web request that caused the exception
     * @return ResponseEntity containing error details and appropriate HTTP status
     */
    @ExceptionHandler(UserServiceException.class)
    public ResponseEntity<ErrorResponse> handleUserServiceException(
            UserServiceException ex, WebRequest request) {
        
        logger.error("User service exception: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            ex.getStatus().value(),
            ex.getStatus().getReasonPhrase(),
            ex.getMessage(),
            request.getDescription(false)
        );
        
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }
    
    /**
     * Handles validation errors from request body validation.
     * This catches @Valid annotation failures and formats them appropriately.
     * 
     * @param ex The MethodArgumentNotValidException containing validation errors
     * @param request The web request that caused the exception
     * @return ResponseEntity containing validation error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            validationErrors.put(error.getField(), error.getDefaultMessage())
        );
        
        logger.error("Validation errors: {}", validationErrors);
        
        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Validation Failed",
            "Request validation failed: " + validationErrors.toString(),
            request.getDescription(false)
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles generic runtime exceptions that are not caught by specific handlers.
     * This is the fallback handler for unexpected errors.
     * 
     * @param ex The generic RuntimeException
     * @param request The web request that caused the exception
     * @return ResponseEntity containing generic error response
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        
        logger.error("Unexpected runtime exception: ", ex);
        
        // Don't expose internal error details in production
        String message = "An unexpected error occurred. Please try again later.";
        
        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            message,
            request.getDescription(false)
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Handles all other exceptions not caught by specific handlers.
     * This ensures no exception goes unhandled.
     * 
     * @param ex The generic Exception
     * @param request The web request that caused the exception
     * @return ResponseEntity containing generic error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        
        logger.error("Unexpected exception: ", ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred. Please contact support if the problem persists.",
            request.getDescription(false)
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}