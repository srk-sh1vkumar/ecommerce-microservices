package com.ecommerce.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested resource cannot be found.
 * Results in HTTP 404 NOT FOUND response.
 */
public class ResourceNotFoundException extends ServiceException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(HttpStatus.NOT_FOUND,
              String.format("%s with identifier '%s' not found", resourceType, identifier),
              "RESOURCE_NOT_FOUND");
    }
}
