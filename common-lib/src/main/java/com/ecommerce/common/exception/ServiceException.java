package com.ecommerce.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception class for all microservice business logic exceptions.
 * Provides a consistent way to handle errors across all services with proper HTTP status codes.
 *
 * Features:
 * - Type-safe exception handling
 * - HTTP status code mapping
 * - Custom error messages
 * - Error code support for client-side handling
 * - Fluent builder pattern for creating exceptions
 *
 * @author E-commerce Development Team
 * @version 2.0
 */
public class ServiceException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final transient Object[] args;

    /**
     * Creates a new service exception with status, message, error code, and arguments.
     *
     * @param status HTTP status code for the error
     * @param message Error message
     * @param errorCode Application-specific error code
     * @param args Optional arguments for message formatting
     */
    public ServiceException(HttpStatus status, String message, String errorCode, Object... args) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.args = args;
    }

    /**
     * Creates a new service exception with status and message.
     *
     * @param status HTTP status code
     * @param message Error message
     */
    public ServiceException(HttpStatus status, String message) {
        this(status, message, null);
    }

    /**
     * Creates a new service exception with status, message, and cause.
     *
     * @param status HTTP status code
     * @param message Error message
     * @param cause The underlying cause
     */
    public ServiceException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = null;
        this.args = null;
    }

    // Factory methods for common HTTP status codes

    /**
     * Creates a BAD_REQUEST (400) exception.
     */
    public static ServiceException badRequest(String message) {
        return new ServiceException(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Creates a BAD_REQUEST (400) exception with error code.
     */
    public static ServiceException badRequest(String message, String errorCode) {
        return new ServiceException(HttpStatus.BAD_REQUEST, message, errorCode);
    }

    /**
     * Creates an UNAUTHORIZED (401) exception.
     */
    public static ServiceException unauthorized(String message) {
        return new ServiceException(HttpStatus.UNAUTHORIZED, message);
    }

    /**
     * Creates an UNAUTHORIZED (401) exception with error code.
     */
    public static ServiceException unauthorized(String message, String errorCode) {
        return new ServiceException(HttpStatus.UNAUTHORIZED, message, errorCode);
    }

    /**
     * Creates a FORBIDDEN (403) exception.
     */
    public static ServiceException forbidden(String message) {
        return new ServiceException(HttpStatus.FORBIDDEN, message);
    }

    /**
     * Creates a NOT_FOUND (404) exception.
     */
    public static ServiceException notFound(String message) {
        return new ServiceException(HttpStatus.NOT_FOUND, message);
    }

    /**
     * Creates a NOT_FOUND (404) exception with error code.
     */
    public static ServiceException notFound(String message, String errorCode) {
        return new ServiceException(HttpStatus.NOT_FOUND, message, errorCode);
    }

    /**
     * Creates a CONFLICT (409) exception.
     */
    public static ServiceException conflict(String message) {
        return new ServiceException(HttpStatus.CONFLICT, message);
    }

    /**
     * Creates a CONFLICT (409) exception with error code.
     */
    public static ServiceException conflict(String message, String errorCode) {
        return new ServiceException(HttpStatus.CONFLICT, message, errorCode);
    }

    /**
     * Creates an INTERNAL_SERVER_ERROR (500) exception.
     */
    public static ServiceException internalError(String message) {
        return new ServiceException(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    /**
     * Creates an INTERNAL_SERVER_ERROR (500) exception with cause.
     */
    public static ServiceException internalError(String message, Throwable cause) {
        return new ServiceException(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }

    /**
     * Creates a SERVICE_UNAVAILABLE (503) exception.
     */
    public static ServiceException serviceUnavailable(String message) {
        return new ServiceException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    /**
     * Creates a GATEWAY_TIMEOUT (504) exception.
     */
    public static ServiceException gatewayTimeout(String message) {
        return new ServiceException(HttpStatus.GATEWAY_TIMEOUT, message);
    }

    // Getters

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args;
    }
}