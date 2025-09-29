package com.ecommerce.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validator implementation for @ValidPassword annotation.
 *
 * Security Features:
 * - Complexity requirements
 * - Common password detection
 * - Sequential character detection
 * - Repeated character detection
 *
 * @author E-commerce Development Team
 * @version 2.0
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");

    private static final Set<String> WEAK_PASSWORDS = Set.of(
        "password", "12345678", "qwerty", "abc123", "letmein",
        "welcome", "monkey", "1234567890", "YOUR_SECURE_PASSWORD", "admin"
    );

    private int minLength;
    private int maxLength;
    private boolean requireUppercase;
    private boolean requireLowercase;
    private boolean requireDigit;
    private boolean requireSpecial;

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        this.minLength = constraintAnnotation.minLength();
        this.maxLength = constraintAnnotation.maxLength();
        this.requireUppercase = constraintAnnotation.requireUppercase();
        this.requireLowercase = constraintAnnotation.requireLowercase();
        this.requireDigit = constraintAnnotation.requireDigit();
        this.requireSpecial = constraintAnnotation.requireSpecial();
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            return true; // Use @NotNull or @NotBlank for required fields
        }

        // Length check
        if (password.length() < minLength) {
            setCustomMessage(context, "Password must be at least " + minLength + " characters long");
            return false;
        }

        if (password.length() > maxLength) {
            setCustomMessage(context, "Password must not exceed " + maxLength + " characters");
            return false;
        }

        // Complexity checks
        if (requireUppercase && !UPPERCASE_PATTERN.matcher(password).find()) {
            setCustomMessage(context, "Password must contain at least one uppercase letter");
            return false;
        }

        if (requireLowercase && !LOWERCASE_PATTERN.matcher(password).find()) {
            setCustomMessage(context, "Password must contain at least one lowercase letter");
            return false;
        }

        if (requireDigit && !DIGIT_PATTERN.matcher(password).find()) {
            setCustomMessage(context, "Password must contain at least one digit");
            return false;
        }

        if (requireSpecial && !SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            setCustomMessage(context, "Password must contain at least one special character");
            return false;
        }

        // Weak password check
        if (isWeakPassword(password)) {
            setCustomMessage(context, "Password is too common or weak");
            return false;
        }

        // Sequential characters check
        if (hasSequentialCharacters(password)) {
            setCustomMessage(context, "Password contains sequential characters");
            return false;
        }

        // Repeated characters check
        if (hasExcessiveRepeatedCharacters(password)) {
            setCustomMessage(context, "Password contains too many repeated characters");
            return false;
        }

        return true;
    }

    private boolean isWeakPassword(String password) {
        String lowerPassword = password.toLowerCase();
        return WEAK_PASSWORDS.stream().anyMatch(lowerPassword::contains);
    }

    private boolean hasSequentialCharacters(String password) {
        // Check for sequences like "abc", "123", "xyz"
        for (int i = 0; i < password.length() - 2; i++) {
            char c1 = password.charAt(i);
            char c2 = password.charAt(i + 1);
            char c3 = password.charAt(i + 2);

            if (c2 == c1 + 1 && c3 == c2 + 1) {
                return true;
            }
        }
        return false;
    }

    private boolean hasExcessiveRepeatedCharacters(String password) {
        // Check for more than 3 consecutive identical characters
        for (int i = 0; i < password.length() - 3; i++) {
            if (password.charAt(i) == password.charAt(i + 1) &&
                password.charAt(i) == password.charAt(i + 2) &&
                password.charAt(i) == password.charAt(i + 3)) {
                return true;
            }
        }
        return false;
    }

    private void setCustomMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}