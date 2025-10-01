package com.ecommerce.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when authentication fails.
 * Results in HTTP 401 UNAUTHORIZED response.
 */
public class AuthenticationException extends ServiceException {

    public AuthenticationException(String message) {
        super(HttpStatus.UNAUTHORIZED, message, "AUTHENTICATION_FAILED");
    }

    public static AuthenticationException invalidCredentials() {
        return new AuthenticationException("Invalid email or password");
    }

    public static AuthenticationException tokenExpired() {
        return new AuthenticationException("Authentication token has expired");
    }

    public static AuthenticationException tokenInvalid() {
        return new AuthenticationException("Invalid authentication token");
    }
}
