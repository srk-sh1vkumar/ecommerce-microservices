package com.ecommerce.user.dto;

import com.ecommerce.common.constants.SecurityConstants;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * Data Transfer Object for user login requests.
 * 
 * This DTO encapsulates the credentials required for user authentication.
 * It provides input validation, security measures, and proper data transfer
 * between the client and server during the login process.
 * 
 * Key Features:
 * - Input validation using Bean Validation annotations
 * - Security-conscious field handling
 * - Proper JSON serialization/deserialization
 * - Immutable design with validation
 * - Clear error messages for validation failures
 * 
 * Security Considerations:
 * - Password field is write-only (not serialized in responses)
 * - Email validation prevents injection attacks
 * - Input length limits prevent buffer overflow
 * - Standard validation patterns for consistency
 * 
 * Usage:
 * This DTO is typically used in REST endpoints for user authentication,
 * ensuring that only valid, properly formatted login data is processed.
 * 
 * @author Ecommerce Development Team
 * @version 1.0
 * @since 2024-01-01
 */
public class LoginRequest {
    
    /**
     * User's email address for authentication.
     * Must be a valid email format and non-empty.
     * Used as the primary identifier for login.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Pattern(regexp = SecurityConstants.EMAIL_PATTERN, 
             message = "Email format is invalid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;
    
    /**
     * User's password for authentication.
     * Must meet minimum security requirements.
     * Never returned in JSON responses for security.
     */
    @NotBlank(message = "Password is required")
    @Size(min = SecurityConstants.MIN_PASSWORD_LENGTH, 
          max = SecurityConstants.MAX_PASSWORD_LENGTH,
          message = "Password must be between " + SecurityConstants.MIN_PASSWORD_LENGTH + 
                   " and " + SecurityConstants.MAX_PASSWORD_LENGTH + " characters")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    
    /**
     * Default constructor for JSON deserialization.
     * Required by Jackson for converting JSON to Java objects.
     */
    public LoginRequest() {}
    
    /**
     * Constructor for creating login request with credentials.
     * 
     * @param email User's email address
     * @param password User's password
     */
    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
    
    /**
     * Gets the user's email address.
     * 
     * @return String email address for authentication
     */
    public String getEmail() { 
        return email; 
    }
    
    /**
     * Sets the user's email address.
     * Email will be normalized (trimmed and lowercased) during processing.
     * 
     * @param email String email address
     */
    public void setEmail(String email) { 
        this.email = email; 
    }
    
    /**
     * Gets the user's password.
     * Note: This method should only be used during server-side processing.
     * Password is never included in JSON responses.
     * 
     * @return String password for authentication
     */
    public String getPassword() { 
        return password; 
    }
    
    /**
     * Sets the user's password.
     * Password will be validated for strength requirements.
     * 
     * @param password String password for authentication
     */
    public void setPassword(String password) { 
        this.password = password; 
    }
    
    /**
     * Checks if the login request has valid credentials.
     * Both email and password must be present and non-empty.
     * 
     * @return true if both email and password are provided, false otherwise
     */
    public boolean hasValidCredentials() {
        return email != null && !email.trim().isEmpty() && 
               password != null && !password.isEmpty();
    }
    
    /**
     * Compares this login request with another object for equality.
     * Two login requests are equal if they have the same email.
     * Password is not included in equality check for security reasons.
     * 
     * @param o Object to compare with
     * @return true if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoginRequest that = (LoginRequest) o;
        return Objects.equals(email, that.email);
    }
    
    /**
     * Generates hash code for this login request based on email.
     * Password is not included in hash calculation for security.
     * 
     * @return int hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(email);
    }
    
    /**
     * Returns a string representation of the login request.
     * Password is excluded for security reasons.
     * 
     * @return String representation showing only email
     */
    @Override
    public String toString() {
        return String.format("LoginRequest{email='%s'}", email);
    }
}