package com.ecommerce.user.service;

import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.dto.AuthResponse;
import com.ecommerce.user.util.JwtUtil;
import com.ecommerce.user.constants.SecurityConstants;
import com.ecommerce.user.exception.UserServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import java.util.Optional;

/**
 * User Service - Core business logic for user management operations.
 * 
 * This service handles all user-related business operations including:
 * - User registration with validation and security
 * - User authentication with JWT token generation
 * - User profile management
 * - Password encoding and validation
 * - Role-based access control
 * 
 * Key Features:
 * - Comprehensive input validation
 * - Secure password handling with BCrypt
 * - JWT token generation for stateless authentication
 * - Transactional operations for data consistency
 * - Detailed logging for audit and debugging
 * - Custom exception handling with meaningful error messages
 * 
 * Security Measures:
 * - Password hashing before storage
 * - Email uniqueness validation
 * - Input sanitization and validation
 * - Secure token generation
 * - No password exposure in responses
 * 
 * Performance Considerations:
 * - Efficient database queries using indexed fields
 * - Minimal data transfer in responses
 * - Caching-friendly operations
 * 
 * @author Ecommerce Development Team
 * @version 1.0
 * @since 2024-01-01
 */
@Service
@Transactional
public class UserService {
    
    /** Logger for this service class */
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    /** Repository for user data access operations */
    private final UserRepository userRepository;
    
    /** Password encoder for secure password hashing */
    private final PasswordEncoder passwordEncoder;
    
    /** JWT utility for token generation and validation */
    private final JwtUtil jwtUtil;
    
    /**
     * Constructor for dependency injection.
     * Uses constructor injection which is the recommended approach for mandatory dependencies.
     * 
     * @param userRepository Repository for user data access
     * @param passwordEncoder Encoder for password hashing
     * @param jwtUtil Utility for JWT operations
     */
    @Autowired
    public UserService(UserRepository userRepository, 
                      PasswordEncoder passwordEncoder, 
                      JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }
    
    /**
     * Authenticates a user and generates a JWT token for subsequent requests.
     * 
     * This method performs the following security operations:
     * 1. Validates user credentials against stored data
     * 2. Compares provided password with hashed password
     * 3. Generates a JWT token for authenticated session
     * 4. Returns user information and token for client use
     * 
     * Security Features:
     * - Secure password comparison using BCrypt
     * - No password exposure in response
     * - Consistent error messages to prevent user enumeration
     * - Audit logging for security monitoring
     * 
     * @param loginRequest Contains email and password for authentication
     * @return AuthResponse containing JWT token and user information
     * @throws UserServiceException if credentials are invalid or user not found
     */
    @Transactional(readOnly = true)
    public AuthResponse login(@Valid LoginRequest loginRequest) {
        logger.info("Login attempt for email: {}", loginRequest.getEmail());
        
        // Validate input parameters
        if (loginRequest == null || !StringUtils.hasText(loginRequest.getEmail()) || 
            !StringUtils.hasText(loginRequest.getPassword())) {
            logger.warn("Login attempt with invalid input data");
            throw UserServiceException.badRequest("Email and password are required");
        }
        
        // Find user by email
        User user = userRepository.findByEmail(loginRequest.getEmail().toLowerCase().trim())
                .orElseThrow(() -> {
                    logger.warn("Login attempt for non-existent email: {}", loginRequest.getEmail());
                    return UserServiceException.unauthorized(SecurityConstants.INVALID_CREDENTIALS);
                });
        
        // Verify password
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            logger.warn("Invalid password attempt for email: {}", loginRequest.getEmail());
            throw UserServiceException.unauthorized(SecurityConstants.INVALID_CREDENTIALS);
        }
        
        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail());
        
        logger.info("Successful login for user: {}", user.getEmail());
        
        // Return authentication response (excluding password)
        return new AuthResponse(token, user.getEmail(), user.getFirstName(), user.getLastName());
    }
    
    /**
     * Registers a new user in the system with comprehensive validation.
     * 
     * This method handles the complete user registration process:
     * 1. Validates user input data
     * 2. Checks for email uniqueness
     * 3. Hashes the password securely
     * 4. Saves user to database
     * 5. Returns sanitized user information
     * 
     * Validation Rules:
     * - Email must be unique across the system
     * - Password must meet security requirements
     * - All required fields must be provided
     * - Email format must be valid
     * 
     * Security Measures:
     * - Password hashing with BCrypt
     * - Email normalization (lowercase, trimmed)
     * - Input sanitization
     * - No password in response
     * 
     * @param user User object containing registration information
     * @return User object with generated ID and sanitized data
     * @throws UserServiceException if validation fails or email already exists
     */
    public User register(@Valid User user) {
        logger.info("Registration attempt for email: {}", user.getEmail());
        
        // Validate input
        if (user == null) {
            throw UserServiceException.badRequest("User data is required");
        }
        
        // Normalize email
        String normalizedEmail = user.getEmail().toLowerCase().trim();
        user.setEmail(normalizedEmail);
        
        // Check if user already exists
        if (userRepository.existsByEmail(normalizedEmail)) {
            logger.warn("Registration attempt for existing email: {}", normalizedEmail);
            throw UserServiceException.conflict(SecurityConstants.USER_ALREADY_EXISTS);
        }
        
        // Validate password strength (additional validation beyond annotations)
        validatePasswordStrength(user.getPassword());
        
        // Hash password before saving
        String hashedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(hashedPassword);
        
        // Set default role if not specified
        if (!StringUtils.hasText(user.getRole())) {
            user.setRole(SecurityConstants.DEFAULT_USER_ROLE);
        }
        
        // Save user to database
        User savedUser = userRepository.save(user);
        
        // Remove password from response for security
        savedUser.setPassword(null);
        
        logger.info("User successfully registered: {}", savedUser.getEmail());
        
        return savedUser;
    }
    
    /**
     * Retrieves a user by their email address.
     * 
     * This method is commonly used for:
     * - Profile information retrieval
     * - Authentication processes
     * - User verification operations
     * 
     * Security Note: Password is excluded from the returned user object.
     * 
     * @param email User's email address (case-insensitive)
     * @return User object without password information
     * @throws UserServiceException if user is not found
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(@NotBlank @Email String email) {
        logger.debug("Retrieving user by email: {}", email);
        
        if (!StringUtils.hasText(email)) {
            throw UserServiceException.badRequest("Email is required");
        }
        
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> {
                    logger.warn("User not found for email: {}", email);
                    return UserServiceException.notFound(SecurityConstants.USER_NOT_FOUND);
                });
        
        // Remove password for security
        user.setPassword(null);
        
        return user;
    }
    
    /**
     * Validates password strength beyond basic length requirements.
     * 
     * Additional security rules:
     * - Must contain at least one uppercase letter
     * - Must contain at least one lowercase letter
     * - Must contain at least one digit
     * - Must not contain common weak passwords
     * 
     * @param password Plain text password to validate
     * @throws UserServiceException if password doesn't meet strength requirements
     */
    private void validatePasswordStrength(String password) {
        if (!StringUtils.hasText(password)) {
            throw UserServiceException.badRequest("Password is required");
        }
        
        // Check for minimum complexity
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        
        if (!hasUpper || !hasLower || !hasDigit) {
            throw UserServiceException.badRequest(
                "Password must contain at least one uppercase letter, one lowercase letter, and one digit");
        }
        
        // Check against common weak passwords
        String[] weakPasswords = {"password", "123456", "qwerty", "admin", "letmein"};
        String lowerPassword = password.toLowerCase();
        
        for (String weak : weakPasswords) {
            if (lowerPassword.contains(weak)) {
                throw UserServiceException.badRequest("Password contains common weak patterns");
            }
        }
    }
    
    /**
     * Updates user profile information.
     * 
     * @param userId ID of the user to update
     * @param updateData User object containing updated information
     * @return Updated user object without password
     * @throws UserServiceException if user not found or validation fails
     */
    public User updateUserProfile(String userId, @Valid User updateData) {
        logger.info("Updating profile for user ID: {}", userId);
        
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> UserServiceException.notFound("User not found"));
        
        // Update allowed fields
        if (StringUtils.hasText(updateData.getFirstName())) {
            existingUser.setFirstName(updateData.getFirstName());
        }
        
        if (StringUtils.hasText(updateData.getLastName())) {
            existingUser.setLastName(updateData.getLastName());
        }
        
        // Email updates require additional validation
        if (StringUtils.hasText(updateData.getEmail()) && 
            !updateData.getEmail().equals(existingUser.getEmail())) {
            
            String normalizedEmail = updateData.getEmail().toLowerCase().trim();
            
            if (userRepository.existsByEmail(normalizedEmail)) {
                throw UserServiceException.conflict("Email already in use");
            }
            
            existingUser.setEmail(normalizedEmail);
        }
        
        User updatedUser = userRepository.save(existingUser);
        updatedUser.setPassword(null); // Remove password from response
        
        logger.info("Profile updated successfully for user: {}", updatedUser.getEmail());
        
        return updatedUser;
    }
}