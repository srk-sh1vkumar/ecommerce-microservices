package com.ecommerce.user.constants;

/**
 * Security-related constants used throughout the User Service.
 * This class contains all the security configurations and constants
 * to ensure consistent security implementation across the application.
 * 
 * @author Ecommerce Development Team
 * @version 1.0
 * @since 2024-01-01
 */
public final class SecurityConstants {
    
    /**
     * Private constructor to prevent instantiation of utility class.
     * This follows the utility class pattern where all methods/fields are static.
     */
    private SecurityConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    // JWT Token Configuration
    /** Default JWT token expiration time in milliseconds (24 hours) */
    public static final int JWT_EXPIRATION = 86400000; // 24 hours
    
    /** JWT token prefix used in Authorization header */
    public static final String TOKEN_PREFIX = "Bearer ";
    
    /** Authorization header name */
    public static final String HEADER_STRING = "Authorization";
    
    // User Role Constants
    /** Default user role assigned to new registrations */
    public static final String DEFAULT_USER_ROLE = "USER";
    
    /** Administrative role with elevated privileges */
    public static final String ADMIN_ROLE = "ADMIN";
    
    // Security Endpoint Patterns
    /** Array of public endpoints that don't require authentication */
    public static final String[] PUBLIC_ENDPOINTS = {
        "/api/users/login",
        "/api/users/register",
        "/actuator/health",
        "/v3/api-docs/**",
        "/swagger-ui/**"
    };
    
    // Validation Constants
    /** Minimum password length for security compliance */
    public static final int MIN_PASSWORD_LENGTH = 8;
    
    /** Maximum password length to prevent buffer overflow attacks */
    public static final int MAX_PASSWORD_LENGTH = 128;
    
    /** Email regex pattern for validation */
    public static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    
    // Error Messages
    /** Error message for invalid credentials */
    public static final String INVALID_CREDENTIALS = "Invalid email or password";
    
    /** Error message when user already exists */
    public static final String USER_ALREADY_EXISTS = "User with this email already exists";
    
    /** Error message when user is not found */
    public static final String USER_NOT_FOUND = "User not found";
    
    /** Error message for invalid token */
    public static final String INVALID_TOKEN = "Invalid or expired token";
}