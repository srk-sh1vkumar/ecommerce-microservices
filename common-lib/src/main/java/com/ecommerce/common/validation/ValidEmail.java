package com.ecommerce.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for email addresses.
 *
 * Features:
 * - RFC 5322 compliant validation
 * - Maximum length check (254 characters)
 * - Domain validation
 * - Disposable email detection (optional)
 *
 * Usage:
 * {@code
 * @ValidEmail
 * private String email;
 * }
 *
 * @author E-commerce Development Team
 * @version 2.0
 */
@Documented
@Constraint(validatedBy = EmailValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmail {

    String message() default "Invalid email address";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    boolean allowDisposable() default true;
}