package com.ecommerce.common.constants;

/**
 * Security-related constants used across all microservices.
 * Centralizes security configuration and validation rules.
 *
 * @author E-commerce Development Team
 * @version 2.0
 */
public final class SecurityConstants {

    private SecurityConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // JWT Configuration
    public static final String JWT_SECRET = "your-256-bit-secret-key-change-this-in-production-environment";
    public static final long JWT_EXPIRATION = 86400000L; // 24 hours in milliseconds
    public static final String JWT_HEADER = "Authorization";
    public static final String JWT_PREFIX = "Bearer ";
    public static final String JWT_ISSUER = "ecommerce-platform";

    // User Roles
    public static final String DEFAULT_USER_ROLE = "USER";
    public static final String ADMIN_ROLE = "ADMIN";
    public static final String VENDOR_ROLE = "VENDOR";

    // Password Validation
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 100;
    public static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$";

    // Email Validation
    public static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    // Error Messages
    public static final String INVALID_CREDENTIALS = "Invalid email or password";
    public static final String USER_ALREADY_EXISTS = "User with this email already exists";
    public static final String USER_NOT_FOUND = "User not found";
    public static final String UNAUTHORIZED_ACCESS = "Unauthorized access";
    public static final String INVALID_TOKEN = "Invalid or expired token";
    public static final String ACCESS_DENIED = "Access denied";

    // API Paths that don't require authentication
    public static final String[] PUBLIC_PATHS = {
            "/api/users/register",
            "/api/users/login",
            "/api/products/**",
            "/actuator/**",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };
}