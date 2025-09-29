package com.ecommerce.user.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.exception.ErrorResponse;
import com.ecommerce.user.dto.AuthResponse;
import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.service.UserServiceRefactored;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Enhanced User Controller with comprehensive API documentation.
 * Manages user registration, authentication, and profile operations.
 *
 * @author E-commerce Development Team
 * @version 2.0
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
@Tag(name = "User Management", description = "APIs for user registration, authentication, and profile management")
public class UserControllerEnhanced {

    private static final Logger logger = LoggerFactory.getLogger(UserControllerEnhanced.class);

    private final UserServiceRefactored userService;

    @Autowired
    public UserControllerEnhanced(UserServiceRefactored userService) {
        this.userService = userService;
    }

    @Operation(
            summary = "User Login",
            description = "Authenticate user with email and password. Returns JWT token for subsequent requests."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request format",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Parameter(description = "Login credentials", required = true)
            @Valid @RequestBody LoginRequest loginRequest) {

        logger.info("Login request for email: {}", loginRequest.getEmail());

        AuthResponse response = userService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @Operation(
            summary = "Register New User",
            description = "Create a new user account. Email must be unique. Password must meet complexity requirements."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "User created successfully",
                    content = @Content(schema = @Schema(implementation = User.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Email already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data or weak password",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(
            @Parameter(description = "User registration data", required = true)
            @Valid @RequestBody User user) {

        logger.info("Registration request for email: {}", user.getEmail());

        User savedUser = userService.register(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(savedUser, "User registered successfully"));
    }

    @Operation(
            summary = "Get User by Email",
            description = "Retrieve user profile information by email address"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User found",
                    content = @Content(schema = @Schema(implementation = User.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{email}")
    public ResponseEntity<ApiResponse<User>> getUserByEmail(
            @Parameter(description = "User email address", required = true, example = "user@example.com")
            @PathVariable String email) {

        logger.info("Get user request for email: {}", email);

        User user = userService.getUserByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @Operation(
            summary = "Update User Profile",
            description = "Update user profile information. Requires authentication."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Profile updated successfully",
                    content = @Content(schema = @Schema(implementation = User.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Email already in use",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<User>> updateProfile(
            @Parameter(description = "User ID", required = true)
            @PathVariable String userId,
            @Parameter(description = "Updated user data", required = true)
            @Valid @RequestBody User updateData) {

        logger.info("Update profile request for userId: {}", userId);

        User updatedUser = userService.updateUserProfile(userId, updateData);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "Profile updated successfully"));
    }

    @Operation(
            summary = "Delete User Account",
            description = "Permanently delete a user account. This action cannot be undone."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "User deleted successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "User ID to delete", required = true)
            @PathVariable String userId) {

        logger.info("Delete user request for userId: {}", userId);

        userService.deleteUser(userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success("User deleted successfully"));
    }

    @Operation(
            summary = "Check Email Availability",
            description = "Check if an email address is available for registration"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Email availability status",
                    content = @Content(schema = @Schema(implementation = Boolean.class))
            )
    })
    @GetMapping("/check-email/{email}")
    public ResponseEntity<ApiResponse<Boolean>> checkEmailAvailability(
            @Parameter(description = "Email to check", required = true, example = "user@example.com")
            @PathVariable String email) {

        logger.debug("Check email availability: {}", email);

        boolean exists = userService.existsByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(
                !exists,
                exists ? "Email already taken" : "Email available"
        ));
    }
}