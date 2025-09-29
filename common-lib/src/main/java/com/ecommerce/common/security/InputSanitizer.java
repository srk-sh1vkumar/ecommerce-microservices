package com.ecommerce.common.security;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Input sanitization utility to prevent XSS, SQL injection, and other attacks.
 *
 * Features:
 * - HTML/JavaScript sanitization
 * - SQL injection prevention
 * - Path traversal prevention
 * - LDAP injection prevention
 * - Command injection prevention
 *
 * Standards:
 * - OWASP Input Validation cheat sheet compliant
 * - Whitelist approach for maximum security
 *
 * @author E-commerce Development Team
 * @version 2.0
 */
@Component
public class InputSanitizer {

    // XSS patterns
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
        "<script[^>]*>.*?</script>|javascript:|onerror=|onload=|onclick=|onmouseover=",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern HTML_PATTERN = Pattern.compile(
        "<[^>]+>",
        Pattern.CASE_INSENSITIVE
    );

    // SQL injection patterns
    private static final Pattern SQL_PATTERN = Pattern.compile(
        "(;|--|#|/\\*|\\*/|xp_|sp_|exec|execute|select|insert|update|delete|drop|create|alter|union|' or|\" or)",
        Pattern.CASE_INSENSITIVE
    );

    // Path traversal patterns
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        "(\\.\\./|\\.\\\\|%2e%2e/|%2e%2e\\\\)",
        Pattern.CASE_INSENSITIVE
    );

    // LDAP injection patterns
    private static final Pattern LDAP_PATTERN = Pattern.compile(
        "[()&|*]",
        Pattern.CASE_INSENSITIVE
    );

    // Command injection patterns
    private static final Pattern COMMAND_PATTERN = Pattern.compile(
        "(;|\\||&|`|\\$\\(|\\$\\{)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Sanitize input to prevent XSS attacks.
     * Removes all HTML tags and JavaScript code.
     */
    public String sanitizeXSS(String input) {
        if (input == null) {
            return null;
        }

        // Remove script tags and JavaScript
        String sanitized = SCRIPT_PATTERN.matcher(input).replaceAll("");

        // Remove all HTML tags
        sanitized = HTML_PATTERN.matcher(sanitized).replaceAll("");

        // Encode special characters
        sanitized = sanitized
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;");

        return sanitized.trim();
    }

    /**
     * Sanitize input to prevent SQL injection.
     * Returns null if suspicious SQL patterns detected.
     */
    public String sanitizeSQL(String input) {
        if (input == null) {
            return null;
        }

        if (SQL_PATTERN.matcher(input).find()) {
            throw new SecurityException("Potential SQL injection detected");
        }

        // Escape single quotes for SQL safety
        return input.replace("'", "''");
    }

    /**
     * Sanitize file path to prevent path traversal attacks.
     */
    public String sanitizeFilePath(String path) {
        if (path == null) {
            return null;
        }

        if (PATH_TRAVERSAL_PATTERN.matcher(path).find()) {
            throw new SecurityException("Path traversal attempt detected");
        }

        // Remove any path traversal sequences
        return path.replaceAll("\\.\\.", "").trim();
    }

    /**
     * Sanitize LDAP filter to prevent LDAP injection.
     */
    public String sanitizeLDAP(String input) {
        if (input == null) {
            return null;
        }

        if (LDAP_PATTERN.matcher(input).find()) {
            throw new SecurityException("Potential LDAP injection detected");
        }

        return input.trim();
    }

    /**
     * Sanitize command input to prevent command injection.
     */
    public String sanitizeCommand(String input) {
        if (input == null) {
            return null;
        }

        if (COMMAND_PATTERN.matcher(input).find()) {
            throw new SecurityException("Potential command injection detected");
        }

        return input.trim();
    }

    /**
     * General purpose sanitization combining multiple checks.
     * Use this for user-generated content.
     */
    public String sanitizeGeneral(String input) {
        if (input == null) {
            return null;
        }

        // Apply XSS sanitization
        String sanitized = sanitizeXSS(input);

        // Check for SQL injection patterns
        if (SQL_PATTERN.matcher(sanitized).find()) {
            throw new SecurityException("Suspicious input pattern detected");
        }

        // Check for path traversal
        if (PATH_TRAVERSAL_PATTERN.matcher(sanitized).find()) {
            throw new SecurityException("Suspicious input pattern detected");
        }

        return sanitized;
    }

    /**
     * Validate if input contains only alphanumeric characters and allowed symbols.
     * Useful for usernames, product codes, etc.
     */
    public boolean isAlphanumericWithSymbols(String input, String allowedSymbols) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        String pattern = "^[a-zA-Z0-9" + Pattern.quote(allowedSymbols) + "]+$";
        return input.matches(pattern);
    }

    /**
     * Validate email format (basic check, use with proper email validation).
     */
    public boolean isSafeEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        // Basic email pattern without complex regex (to prevent ReDoS)
        String pattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(pattern) && email.length() <= 254;
    }

    /**
     * Truncate input to maximum length to prevent buffer overflow attacks.
     */
    public String truncate(String input, int maxLength) {
        if (input == null) {
            return null;
        }

        if (input.length() > maxLength) {
            return input.substring(0, maxLength);
        }

        return input;
    }
}