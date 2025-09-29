package com.ecommerce.user.exception;

import org.springframework.http.HttpStatus;

/**
 * Custom exception class for User Service specific errors.
 * This exception is used to handle business logic errors and provides
 * better error handling with appropriate HTTP status codes.
 * 
 * Features:
 * - Includes HTTP status code for proper REST API responses
 * - Provides meaningful error messages for different scenarios
 * - Extends RuntimeException for unchecked exception handling
 * - Supports both simple and detailed error messages
 * 
 * Usage Examples:
 * - User not found: throw new UserServiceException("User not found", HttpStatus.NOT_FOUND)
 * - Invalid credentials: throw new UserServiceException("Invalid credentials", HttpStatus.UNAUTHORIZED)
 * - User already exists: throw new UserServiceException("User already exists", HttpStatus.CONFLICT)
 * 
 * @author Ecommerce Development Team
 * @version 1.0
 * @since 2024-01-01
 */
public class UserServiceException extends RuntimeException {
    
    /** The HTTP status code associated with this exception */
    private final HttpStatus status;
    
    /**
     * Constructs a new UserServiceException with the specified detail message and HTTP status.
     * 
     * @param message The detail message explaining the cause of the exception
     * @param status The HTTP status code that should be returned to the client
     */
    public UserServiceException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
    
    /**
     * Constructs a new UserServiceException with the specified detail message, 
     * HTTP status, and cause.
     * 
     * @param message The detail message explaining the cause of the exception
     * @param status The HTTP status code that should be returned to the client
     * @param cause The underlying cause of this exception
     */
    public UserServiceException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
    
    /**
     * Returns the HTTP status code associated with this exception.
     * This is used by the GlobalExceptionHandler to set the appropriate
     * HTTP response status.
     * 
     * @return The HttpStatus that should be returned to the client
     */
    public HttpStatus getStatus() {
        return status;
    }
    
    /**
     * Static factory method for creating a NOT_FOUND exception.
     * Commonly used when a user cannot be found by email or ID.
     * 
     * @param message The error message
     * @return A new UserServiceException with NOT_FOUND status
     */
    public static UserServiceException notFound(String message) {
        return new UserServiceException(message, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Static factory method for creating an UNAUTHORIZED exception.
     * Commonly used for authentication failures.
     * 
     * @param message The error message
     * @return A new UserServiceException with UNAUTHORIZED status
     */
    public static UserServiceException unauthorized(String message) {
        return new UserServiceException(message, HttpStatus.UNAUTHORIZED);
    }
    
    /**
     * Static factory method for creating a CONFLICT exception.
     * Commonly used when trying to create a user that already exists.
     * 
     * @param message The error message
     * @return A new UserServiceException with CONFLICT status
     */
    public static UserServiceException conflict(String message) {
        return new UserServiceException(message, HttpStatus.CONFLICT);
    }
    
    /**
     * Static factory method for creating a BAD_REQUEST exception.
     * Commonly used for validation failures or malformed requests.
     * 
     * @param message The error message
     * @return A new UserServiceException with BAD_REQUEST status
     */
    public static UserServiceException badRequest(String message) {
        return new UserServiceException(message, HttpStatus.BAD_REQUEST);
    }
}