package com.ecommerce.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Data Transfer Object for authentication responses.
 * 
 * This DTO encapsulates the information returned to clients after successful
 * authentication, including the JWT token and user profile information.
 * It provides a secure and structured way to return authentication data
 * without exposing sensitive information.
 * 
 * Key Features:
 * - Secure token delivery
 * - Essential user profile information
 * - Token expiration information
 * - JSON serialization optimization
 * - Security-conscious data exposure
 * 
 * Security Considerations:
 * - Only includes non-sensitive user information
 * - Token should be handled securely by clients
 * - No password or internal IDs exposed
 * - Expiration time helps with token management
 * 
 * Usage:
 * This DTO is returned by authentication endpoints to provide
 * clients with the necessary information for authenticated sessions.
 * 
 * @author Ecommerce Development Team
 * @version 1.0
 * @since 2024-01-01
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    
    /**
     * JWT authentication token for subsequent API requests.
     * This token should be included in the Authorization header
     * for authenticated endpoints.
     */
    @JsonProperty("access_token")
    private String token;
    
    /**
     * Token type - typically "Bearer" for JWT tokens.
     * Helps clients understand how to use the token.
     */
    @JsonProperty("token_type")
    private String tokenType = "Bearer";
    
    /**
     * Token expiration time in seconds.
     * Helps clients manage token refresh and logout.
     */
    @JsonProperty("expires_in")
    private long expiresIn;
    
    /**
     * User's email address - serves as the unique identifier.
     */
    private String email;
    
    /**
     * User's first name for personalization.
     */
    private String firstName;
    
    /**
     * User's last name for complete identification.
     */
    private String lastName;
    
    /**
     * User's role in the system for client-side authorization.
     */
    private String role;
    
    /**
     * Timestamp when the authentication occurred.
     * Useful for audit trails and session management.
     */
    private LocalDateTime authenticatedAt;
    
    /**
     * Default constructor for JSON deserialization.
     */
    public AuthResponse() {
        this.authenticatedAt = LocalDateTime.now();
    }
    
    /**
     * Constructor for creating authentication response with essential information.
     * 
     * @param token JWT authentication token
     * @param email User's email address
     * @param firstName User's first name
     * @param lastName User's last name
     */
    public AuthResponse(String token, String email, String firstName, String lastName) {
        this();
        this.token = token;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }
    
    /**
     * Constructor for creating complete authentication response.
     * 
     * @param token JWT authentication token
     * @param email User's email address
     * @param firstName User's first name
     * @param lastName User's last name
     * @param role User's role in the system
     * @param expiresIn Token expiration time in seconds
     */
    public AuthResponse(String token, String email, String firstName, String lastName, 
                       String role, long expiresIn) {
        this(token, email, firstName, lastName);
        this.role = role;
        this.expiresIn = expiresIn;
    }
    
    /**
     * Gets the JWT authentication token.
     * 
     * @return String JWT token for API authentication
     */
    public String getToken() { 
        return token; 
    }
    
    /**
     * Sets the JWT authentication token.
     * 
     * @param token String JWT token
     */
    public void setToken(String token) { 
        this.token = token; 
    }
    
    /**
     * Gets the token type.
     * 
     * @return String token type (typically "Bearer")
     */
    public String getTokenType() {
        return tokenType;
    }
    
    /**
     * Sets the token type.
     * 
     * @param tokenType String token type
     */
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
    
    /**
     * Gets the token expiration time in seconds.
     * 
     * @return long expiration time in seconds
     */
    public long getExpiresIn() {
        return expiresIn;
    }
    
    /**
     * Sets the token expiration time in seconds.
     * 
     * @param expiresIn long expiration time in seconds
     */
    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
    
    /**
     * Gets the user's email address.
     * 
     * @return String user's email
     */
    public String getEmail() { 
        return email; 
    }
    
    /**
     * Sets the user's email address.
     * 
     * @param email String user's email
     */
    public void setEmail(String email) { 
        this.email = email; 
    }
    
    /**
     * Gets the user's first name.
     * 
     * @return String user's first name
     */
    public String getFirstName() { 
        return firstName; 
    }
    
    /**
     * Sets the user's first name.
     * 
     * @param firstName String user's first name
     */
    public void setFirstName(String firstName) { 
        this.firstName = firstName; 
    }
    
    /**
     * Gets the user's last name.
     * 
     * @return String user's last name
     */
    public String getLastName() { 
        return lastName; 
    }
    
    /**
     * Sets the user's last name.
     * 
     * @param lastName String user's last name
     */
    public void setLastName(String lastName) { 
        this.lastName = lastName; 
    }
    
    /**
     * Gets the user's role in the system.
     * 
     * @return String user's role
     */
    public String getRole() {
        return role;
    }
    
    /**
     * Sets the user's role in the system.
     * 
     * @param role String user's role
     */
    public void setRole(String role) {
        this.role = role;
    }
    
    /**
     * Gets the authentication timestamp.
     * 
     * @return LocalDateTime when authentication occurred
     */
    public LocalDateTime getAuthenticatedAt() {
        return authenticatedAt;
    }
    
    /**
     * Sets the authentication timestamp.
     * 
     * @param authenticatedAt LocalDateTime when authentication occurred
     */
    public void setAuthenticatedAt(LocalDateTime authenticatedAt) {
        this.authenticatedAt = authenticatedAt;
    }
    
    /**
     * Gets the user's full name by combining first and last names.
     * 
     * @return String full name in "FirstName LastName" format
     */
    @JsonProperty("full_name")
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return firstName != null ? firstName : lastName;
    }
    
    /**
     * Checks if the authentication response is valid.
     * A valid response must have a token and email.
     * 
     * @return true if response has required fields, false otherwise
     */
    public boolean isValid() {
        return token != null && !token.trim().isEmpty() && 
               email != null && !email.trim().isEmpty();
    }
    
    /**
     * Compares this authentication response with another object for equality.
     * 
     * @param o Object to compare with
     * @return true if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthResponse that = (AuthResponse) o;
        return Objects.equals(email, that.email) && 
               Objects.equals(token, that.token);
    }
    
    /**
     * Generates hash code for this authentication response.
     * 
     * @return int hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(email, token);
    }
    
    /**
     * Returns a string representation of the authentication response.
     * Token is excluded for security reasons.
     * 
     * @return String representation without sensitive data
     */
    @Override
    public String toString() {
        return String.format("AuthResponse{email='%s', firstName='%s', lastName='%s', role='%s', authenticatedAt=%s}",
                email, firstName, lastName, role, authenticatedAt);
    }
}