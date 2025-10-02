package com.ecommerce.user.service;

import com.ecommerce.common.constants.ErrorCodes;
import com.ecommerce.common.constants.SecurityConstants;
import com.ecommerce.common.exception.ServiceException;
import com.ecommerce.common.util.JwtUtil;
import com.ecommerce.common.util.ValidationUtils;
import com.ecommerce.user.dto.AuthResponse;
import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.dto.UserResponseDTO;
import com.ecommerce.common.metrics.MetricsService;
import com.ecommerce.user.client.NotificationServiceClient;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.mapper.UserMapper;
import com.ecommerce.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refactored User Service using common library utilities.
 * Implements user management with improved architecture and reusable components.
 *
 * Improvements:
 * - Uses common exception handling framework
 * - Leverages shared validation utilities
 * - Centralized JWT token management
 * - Consistent error codes across services
 * - Better separation of concerns
 * - Enhanced logging and monitoring
 *
 * @author E-commerce Development Team
 * @version 2.0
 */
@Service
@Transactional
public class UserServiceRefactored {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceRefactored.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final MetricsService metricsService;
    private final NotificationServiceClient notificationServiceClient;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Autowired
    public UserServiceRefactored(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 JwtUtil jwtUtil,
                                 UserMapper userMapper,
                                 MetricsService metricsService,
                                 NotificationServiceClient notificationServiceClient) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
        this.metricsService = metricsService;
        this.notificationServiceClient = notificationServiceClient;
    }

    /**
     * Authenticates a user and generates a JWT token.
     *
     * @param loginRequest Contains email and password
     * @return AuthResponse with JWT token and user info
     * @throws ServiceException if authentication fails
     */
    @Transactional(readOnly = true)
    public AuthResponse login(@Valid LoginRequest loginRequest) {
        logger.info("Login attempt for email: {}", loginRequest.getEmail());

        // Validate input using common utilities
        ValidationUtils.validateEmail(loginRequest.getEmail());
        ValidationUtils.validateNotBlank(loginRequest.getPassword(), "Password");

        // Normalize email
        String normalizedEmail = ValidationUtils.normalizeEmail(loginRequest.getEmail());

        // Find user
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    logger.warn("Login attempt for non-existent email: {}", normalizedEmail);
                    return ServiceException.unauthorized(
                            SecurityConstants.INVALID_CREDENTIALS,
                            ErrorCodes.INVALID_CREDENTIALS
                    );
                });

        // Verify password
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            logger.warn("Invalid password attempt for email: {}", normalizedEmail);
            metricsService.incrementFailedLogins();
            throw ServiceException.unauthorized(
                    SecurityConstants.INVALID_CREDENTIALS,
                    ErrorCodes.INVALID_CREDENTIALS
            );
        }

        // Generate token with role
        String token = jwtUtil.generateTokenWithRole(user.getEmail(), user.getRole());

        logger.info("Successful login for user: {}", user.getEmail());
        metricsService.incrementUserLogins();

        return new AuthResponse(token, user.getEmail(), user.getFirstName(), user.getLastName());
    }

    /**
     * Registers a new user in the system.
     *
     * @param user User registration data
     * @return UserResponseDTO without password
     * @throws ServiceException if validation fails or email exists
     */
    public UserResponseDTO register(@Valid User user) {
        logger.info("Registration attempt for email: {}", user.getEmail());

        // Validate input using common utilities
        ValidationUtils.validateEmail(user.getEmail());
        ValidationUtils.validatePassword(user.getPassword());
        ValidationUtils.validateNotBlank(user.getFirstName(), "First name");
        ValidationUtils.validateNotBlank(user.getLastName(), "Last name");

        // Normalize email
        String normalizedEmail = ValidationUtils.normalizeEmail(user.getEmail());
        user.setEmail(normalizedEmail);

        // Check if user already exists
        if (userRepository.existsByEmail(normalizedEmail)) {
            logger.warn("Registration attempt for existing email: {}", normalizedEmail);
            throw ServiceException.conflict(
                    SecurityConstants.USER_ALREADY_EXISTS,
                    ErrorCodes.USER_ALREADY_EXISTS
            );
        }

        // Hash password
        String hashedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(hashedPassword);

        // Set default role if not specified
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole(SecurityConstants.DEFAULT_USER_ROLE);
        }

        // Save user
        User savedUser = userRepository.save(user);

        logger.info("User successfully registered: {}", savedUser.getEmail());
        metricsService.incrementUserRegistrations();

        // Send welcome email asynchronously
        sendWelcomeEmailAsync(savedUser);

        // Convert to DTO (automatically excludes password)
        return userMapper.toResponseDTO(savedUser);
    }

    /**
     * Sends a welcome email to a newly registered user asynchronously.
     * Failures in email sending do not affect the registration process.
     *
     * @param user Newly registered user
     */
    @Async
    protected void sendWelcomeEmailAsync(User user) {
        try {
            String fullName = user.getFirstName() + " " + user.getLastName();
            String activationLink = baseUrl + "/api/users/activate?token=" +
                                  jwtUtil.generateToken(user.getEmail());

            notificationServiceClient.sendWelcomeEmail(
                user.getEmail(),
                fullName,
                activationLink
            );

            logger.info("Welcome email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            // Log error but don't fail registration
            logger.error("Failed to send welcome email to: {}. Error: {}",
                        user.getEmail(), e.getMessage());
        }
    }

    /**
     * Retrieves a user by email address.
     *
     * @param email User's email
     * @return UserResponseDTO without password
     * @throws ServiceException if user not found
     */
    @Cacheable(value = "userByEmail", key = "#email")
    @Transactional(readOnly = true)
    public UserResponseDTO getUserByEmail(String email) {
        logger.debug("Retrieving user by email: {}", email);

        ValidationUtils.validateEmail(email);
        String normalizedEmail = ValidationUtils.normalizeEmail(email);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    logger.warn("User not found for email: {}", normalizedEmail);
                    return ServiceException.notFound(
                            SecurityConstants.USER_NOT_FOUND,
                            ErrorCodes.USER_NOT_FOUND
                    );
                });

        // Convert to DTO (automatically excludes password)
        return userMapper.toResponseDTO(user);
    }

    /**
     * Updates user profile information.
     *
     * @param userId User ID
     * @param updateData Updated user data
     * @return UserResponseDTO without password
     * @throws ServiceException if user not found or validation fails
     */
    @CacheEvict(value = {"user", "userByEmail"}, allEntries = true)
    public UserResponseDTO updateUserProfile(String userId, @Valid User updateData) {
        logger.info("Updating profile for user ID: {}", userId);

        ValidationUtils.validateId(userId, "User");

        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> ServiceException.notFound(
                        "User not found",
                        ErrorCodes.USER_NOT_FOUND
                ));

        // Update allowed fields
        if (updateData.getFirstName() != null && !updateData.getFirstName().isEmpty()) {
            ValidationUtils.validateNotBlank(updateData.getFirstName(), "First name");
            existingUser.setFirstName(updateData.getFirstName());
        }

        if (updateData.getLastName() != null && !updateData.getLastName().isEmpty()) {
            ValidationUtils.validateNotBlank(updateData.getLastName(), "Last name");
            existingUser.setLastName(updateData.getLastName());
        }

        // Email updates require additional validation
        if (updateData.getEmail() != null &&
                !updateData.getEmail().equals(existingUser.getEmail())) {

            ValidationUtils.validateEmail(updateData.getEmail());
            String normalizedEmail = ValidationUtils.normalizeEmail(updateData.getEmail());

            if (userRepository.existsByEmail(normalizedEmail)) {
                throw ServiceException.conflict(
                        "Email already in use",
                        ErrorCodes.USER_ALREADY_EXISTS
                );
            }

            existingUser.setEmail(normalizedEmail);
        }

        User updatedUser = userRepository.save(existingUser);

        logger.info("Profile updated successfully for user: {}", updatedUser.getEmail());

        // Convert to DTO (automatically excludes password)
        return userMapper.toResponseDTO(updatedUser);
    }

    /**
     * Deletes a user account.
     *
     * @param userId User ID to delete
     * @throws ServiceException if user not found
     */
    @CacheEvict(value = {"user", "userByEmail", "emailExists"}, allEntries = true)
    public void deleteUser(String userId) {
        logger.info("Deleting user with ID: {}", userId);

        ValidationUtils.validateId(userId, "User");

        if (!userRepository.existsById(userId)) {
            throw ServiceException.notFound(
                    "User not found",
                    ErrorCodes.USER_NOT_FOUND
            );
        }

        userRepository.deleteById(userId);
        logger.info("User deleted successfully: {}", userId);
    }

    /**
     * Checks if a user exists by email.
     *
     * @param email User email
     * @return true if user exists
     */
    @Cacheable(value = "emailExists", key = "#email")
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        ValidationUtils.validateEmail(email);
        String normalizedEmail = ValidationUtils.normalizeEmail(email);
        return userRepository.existsByEmail(normalizedEmail);
    }

    /**
     * Initiates password reset process by sending a reset email.
     *
     * @param email User's email address
     * @throws ServiceException if user not found
     */
    @Transactional(readOnly = true)
    public void requestPasswordReset(String email) {
        logger.info("Password reset requested for email: {}", email);

        ValidationUtils.validateEmail(email);
        String normalizedEmail = ValidationUtils.normalizeEmail(email);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    logger.warn("Password reset requested for non-existent email: {}", normalizedEmail);
                    return ServiceException.notFound(
                            "User not found",
                            ErrorCodes.USER_NOT_FOUND
                    );
                });

        // Send password reset email asynchronously
        sendPasswordResetEmailAsync(user);
    }

    /**
     * Sends a password reset email asynchronously.
     *
     * @param user User requesting password reset
     */
    @Async
    protected void sendPasswordResetEmailAsync(User user) {
        try {
            String fullName = user.getFirstName() + " " + user.getLastName();
            // Generate time-limited reset token (expires in 24 hours)
            String resetToken = jwtUtil.generateToken(user.getEmail());
            String resetLink = baseUrl + "/api/users/reset-password?token=" + resetToken;

            notificationServiceClient.sendPasswordResetEmail(
                user.getEmail(),
                fullName,
                resetLink
            );

            logger.info("Password reset email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send password reset email to: {}. Error: {}",
                        user.getEmail(), e.getMessage());
        }
    }
}