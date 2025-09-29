package com.ecommerce.common.util;

import com.ecommerce.common.constants.ErrorCodes;
import com.ecommerce.common.exception.ServiceException;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Utility class for common validation operations across microservices.
 * Provides reusable validation logic with consistent error handling.
 *
 * @author E-commerce Development Team
 * @version 2.0
 */
public final class ValidationUtils {

    private ValidationUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$"
    );

    /**
     * Validates that a string is not null or empty.
     *
     * @param value The string to validate
     * @param fieldName The name of the field for error messages
     * @throws ServiceException if validation fails
     */
    public static void validateNotBlank(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw ServiceException.badRequest(
                    fieldName + " is required",
                    ErrorCodes.VALIDATION_ERROR
            );
        }
    }

    /**
     * Validates email format.
     *
     * @param email The email to validate
     * @throws ServiceException if email is invalid
     */
    public static void validateEmail(String email) {
        validateNotBlank(email, "Email");

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw ServiceException.badRequest(
                    "Invalid email format",
                    ErrorCodes.INVALID_EMAIL
            );
        }
    }

    /**
     * Validates password strength.
     *
     * @param password The password to validate
     * @throws ServiceException if password doesn't meet requirements
     */
    public static void validatePassword(String password) {
        validateNotBlank(password, "Password");

        if (password.length() < 8 || password.length() > 100) {
            throw ServiceException.badRequest(
                    "Password must be between 8 and 100 characters",
                    ErrorCodes.WEAK_PASSWORD
            );
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw ServiceException.badRequest(
                    "Password must contain at least one uppercase letter, one lowercase letter, and one digit",
                    ErrorCodes.WEAK_PASSWORD
            );
        }

        // Check for common weak passwords
        String lowerPassword = password.toLowerCase();
        String[] weakPasswords = {"password", "123456", "qwerty", "admin", "letmein", "welcome"};

        for (String weak : weakPasswords) {
            if (lowerPassword.contains(weak)) {
                throw ServiceException.badRequest(
                        "Password contains common weak patterns",
                        ErrorCodes.WEAK_PASSWORD
                );
            }
        }
    }

    /**
     * Validates that a number is positive.
     *
     * @param value The number to validate
     * @param fieldName The name of the field
     * @throws ServiceException if number is not positive
     */
    public static void validatePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw ServiceException.badRequest(
                    fieldName + " must be a positive number",
                    ErrorCodes.VALIDATION_ERROR
            );
        }
    }

    /**
     * Validates that a BigDecimal price is valid.
     *
     * @param price The price to validate
     * @throws ServiceException if price is invalid
     */
    public static void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw ServiceException.badRequest(
                    "Price must be greater than zero",
                    ErrorCodes.INVALID_PRICE
            );
        }

        if (price.scale() > 2) {
            throw ServiceException.badRequest(
                    "Price cannot have more than 2 decimal places",
                    ErrorCodes.INVALID_PRICE
            );
        }
    }

    /**
     * Validates quantity for cart/order operations.
     *
     * @param quantity The quantity to validate
     * @throws ServiceException if quantity is invalid
     */
    public static void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw ServiceException.badRequest(
                    "Quantity must be at least 1",
                    ErrorCodes.INVALID_QUANTITY
            );
        }

        if (quantity > 100) {
            throw ServiceException.badRequest(
                    "Quantity cannot exceed 100 items",
                    ErrorCodes.INVALID_QUANTITY
            );
        }
    }

    /**
     * Validates that sufficient stock is available.
     *
     * @param requestedQuantity The requested quantity
     * @param availableStock The available stock
     * @throws ServiceException if insufficient stock
     */
    public static void validateStock(Integer requestedQuantity, Integer availableStock) {
        if (availableStock == null || availableStock < requestedQuantity) {
            throw ServiceException.badRequest(
                    "Insufficient stock. Available: " + (availableStock != null ? availableStock : 0),
                    ErrorCodes.INSUFFICIENT_STOCK
            );
        }
    }

    /**
     * Normalizes email address (lowercase and trim).
     *
     * @param email The email to normalize
     * @return Normalized email address
     */
    public static String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return email.toLowerCase().trim();
    }

    /**
     * Validates ID format (non-null and non-empty).
     *
     * @param id The ID to validate
     * @param entityName The name of the entity
     * @throws ServiceException if ID is invalid
     */
    public static void validateId(String id, String entityName) {
        if (!StringUtils.hasText(id)) {
            throw ServiceException.badRequest(
                    entityName + " ID is required",
                    ErrorCodes.VALIDATION_ERROR
            );
        }
    }
}