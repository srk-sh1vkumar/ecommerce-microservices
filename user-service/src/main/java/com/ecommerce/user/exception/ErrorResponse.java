package com.ecommerce.user.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * Standardized error response structure for the User Service.
 * This class provides a consistent format for all error responses
 * across the application, making it easier for clients to parse
 * and handle errors appropriately.
 * 
 * The error response follows REST API best practices by including:
 * - Timestamp of when the error occurred
 * - HTTP status code for programmatic handling
 * - Human-readable error message
 * - Request path for debugging purposes
 * 
 * This standardization helps with:
 * - Frontend error handling
 * - API documentation
 * - Debugging and monitoring
 * - Consistent user experience
 * 
 * @author Ecommerce Development Team
 * @version 1.0
 * @since 2024-01-01
 */
public class ErrorResponse {
    
    /** 
     * Timestamp when the error occurred.
     * Formatted as ISO 8601 string for better readability and standardization.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    /** HTTP status code (e.g., 400, 404, 500) */
    private int status;
    
    /** HTTP status reason phrase (e.g., "Bad Request", "Not Found") */
    private String error;
    
    /** Detailed error message explaining what went wrong */
    private String message;
    
    /** The request path that caused the error (useful for debugging) */
    private String path;
    
    /**
     * Default constructor for JSON deserialization.
     * Required by Jackson for converting JSON to Java objects.
     */
    public ErrorResponse() {}
    
    /**
     * Constructor for creating a complete error response.
     * This is the primary constructor used by exception handlers.
     * 
     * @param timestamp When the error occurred
     * @param status HTTP status code
     * @param error HTTP status reason phrase
     * @param message Detailed error message
     * @param path Request path that caused the error
     */
    public ErrorResponse(LocalDateTime timestamp, int status, String error, 
                        String message, String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }
    
    // Getter and Setter methods with JavaDoc documentation
    
    /**
     * Gets the timestamp when the error occurred.
     * 
     * @return LocalDateTime representing when the error happened
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * Sets the timestamp when the error occurred.
     * 
     * @param timestamp LocalDateTime when the error happened
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Gets the HTTP status code.
     * 
     * @return Integer representing the HTTP status code
     */
    public int getStatus() {
        return status;
    }
    
    /**
     * Sets the HTTP status code.
     * 
     * @param status Integer HTTP status code
     */
    public void setStatus(int status) {
        this.status = status;
    }
    
    /**
     * Gets the HTTP status reason phrase.
     * 
     * @return String representing the HTTP status reason
     */
    public String getError() {
        return error;
    }
    
    /**
     * Sets the HTTP status reason phrase.
     * 
     * @param error String HTTP status reason phrase
     */
    public void setError(String error) {
        this.error = error;
    }
    
    /**
     * Gets the detailed error message.
     * 
     * @return String containing the error details
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Sets the detailed error message.
     * 
     * @param message String containing error details
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * Gets the request path that caused the error.
     * 
     * @return String representing the request path
     */
    public String getPath() {
        return path;
    }
    
    /**
     * Sets the request path that caused the error.
     * 
     * @param path String representing the request path
     */
    public void setPath(String path) {
        this.path = path;
    }
    
    /**
     * Returns a string representation of the error response.
     * Useful for logging and debugging purposes.
     * 
     * @return String representation of the error
     */
    @Override
    public String toString() {
        return String.format("ErrorResponse{timestamp=%s, status=%d, error='%s', message='%s', path='%s'}",
                timestamp, status, error, message, path);
    }
}