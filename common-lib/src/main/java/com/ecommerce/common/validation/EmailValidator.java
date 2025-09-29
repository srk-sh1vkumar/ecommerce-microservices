package com.ecommerce.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validator implementation for @ValidEmail annotation.
 *
 * Validation Rules:
 * - RFC 5322 email format
 * - Maximum length: 254 characters
 * - Valid domain structure
 * - Optional disposable email check
 *
 * @author E-commerce Development Team
 * @version 2.0
 */
public class EmailValidator implements ConstraintValidator<ValidEmail, String> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
        "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$"
    );

    private static final int MAX_EMAIL_LENGTH = 254;

    private static final Set<String> DISPOSABLE_DOMAINS = Set.of(
        "tempmail.com", "throwaway.email", "guerrillamail.com",
        "10minutemail.com", "mailinator.com", "trashmail.com"
    );

    private boolean allowDisposable;

    @Override
    public void initialize(ValidEmail constraintAnnotation) {
        this.allowDisposable = constraintAnnotation.allowDisposable();
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.isEmpty()) {
            return true; // Use @NotNull or @NotBlank for required fields
        }

        // Length check
        if (email.length() > MAX_EMAIL_LENGTH) {
            setCustomMessage(context, "Email address too long (max 254 characters)");
            return false;
        }

        // Format check
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            setCustomMessage(context, "Invalid email format");
            return false;
        }

        // Disposable email check
        if (!allowDisposable && isDisposableEmail(email)) {
            setCustomMessage(context, "Disposable email addresses are not allowed");
            return false;
        }

        return true;
    }

    private boolean isDisposableEmail(String email) {
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase();
        return DISPOSABLE_DOMAINS.contains(domain);
    }

    private void setCustomMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}