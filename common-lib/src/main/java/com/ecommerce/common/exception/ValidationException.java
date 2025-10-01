package com.ecommerce.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when business validation fails.
 * Results in HTTP 400 BAD REQUEST response.
 */
public class ValidationException extends ServiceException {

    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, message, "VALIDATION_ERROR");
    }

    public ValidationException(String field, String reason) {
        super(HttpStatus.BAD_REQUEST,
              String.format("Validation failed for field '%s': %s", field, reason),
              "VALIDATION_ERROR");
    }
}
