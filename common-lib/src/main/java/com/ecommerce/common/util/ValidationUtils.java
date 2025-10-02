package com.ecommerce.common.util;

import com.ecommerce.common.constants.ErrorCodes;
import com.ecommerce.common.exception.ServiceException;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Comprehensive validation utility class for e-commerce microservices.
 *
 * <p>This utility class provides centralized, reusable validation methods used across
 * all microservices in the e-commerce platform. It ensures consistent validation logic
 * and error handling throughout the system.</p>
 *
 * <p><b>Validation Categories:</b></p>
 * <ul>
 *   <li><b>String Validation:</b> Not blank, min/max length checks</li>
 *   <li><b>Email Validation:</b> Format validation with regex pattern</li>
 *   <li><b>Password Validation:</b> Strength requirements (length, complexity)</li>
 *   <li><b>Numeric Validation:</b> Positive values, price validation</li>
 *   <li><b>ID Validation:</b> Non-null, non-empty resource identifiers</li>
 *   <li><b>Stock Validation:</b> Availability checks</li>
 * </ul>
 *
 * <p><b>Error Handling:</b></p>
 * <p>All validation methods throw {@link ServiceException} with appropriate HTTP status
 * codes and standardized error codes for consistent API responses.</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * // Validate email
 * ValidationUtils.validateEmail("user@example.com");
 *
 * // Validate password strength
 * ValidationUtils.validatePassword("SecureP@ss123");
 *
 * // Validate positive integer
 * ValidationUtils.validatePositive(quantity, "Quantity");
 *
 * // Validate price
 * ValidationUtils.validatePrice(new BigDecimal("29.99"));
 * }</pre>
 *
 * @author E-commerce Development Team
 * @version 2.0
 * @since 1.0
 * @see ServiceException
 * @see ErrorCodes
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
     * Validates that a string is not null, empty, or whitespace-only.
     *
     * <p>This method uses Spring's {@code StringUtils.hasText()} to check if the
     * value contains actual text content.</p>
     *
     * @param value the string value to validate
     * @param fieldName the name of the field being validated (used in error message)
     * @throws ServiceException with HTTP 400 Bad Request if the value is blank
     * @see #validateEmail(String)
     * @see #validateId(String, String)
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
     * Validates email address format using regex pattern.
     *
     * <p>The email must match the pattern: {@code ^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$}</p>
     *
     * <p><b>Valid Examples:</b></p>
     * <ul>
     *   <li>user@example.com</li>
     *   <li>john.doe+tag@company.co.uk</li>
     *   <li>test_123@test-domain.com</li>
     * </ul>
     *
     * @param email the email address to validate, must not be null or empty
     * @throws ServiceException with HTTP 400 Bad Request if email format is invalid
     * @see #normalizeEmail(String)
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
     * Validates password strength according to security requirements.
     *
     * <p><b>Password Requirements:</b></p>
     * <ul>
     *   <li>Minimum 8 characters, maximum 100 characters</li>
     *   <li>At least one lowercase letter (a-z)</li>
     *   <li>At least one uppercase letter (A-Z)</li>
     *   <li>At least one digit (0-9)</li>
     * </ul>
     *
     * <p><b>Valid Examples:</b></p>
     * <ul>
     *   <li>SecurePass123</li>
     *   <li>MyP@ssw0rd!</li>
     *   <li>Admin2024Test</li>
     * </ul>
     *
     * @param password the password to validate, must not be null or empty
     * @throws ServiceException with HTTP 400 Bad Request if password doesn't meet requirements
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