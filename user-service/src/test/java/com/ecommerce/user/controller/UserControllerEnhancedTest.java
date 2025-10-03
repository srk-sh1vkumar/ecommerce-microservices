package com.ecommerce.user.controller;

import com.ecommerce.common.exception.ServiceException;
import com.ecommerce.user.dto.AuthResponse;
import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.dto.UserResponseDTO;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.service.UserServiceRefactored;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller layer tests for UserControllerEnhanced.
 * Tests HTTP request/response handling, validation, and error responses.
 *
 * @author E-commerce Development Team
 * @version 1.0
 */
@WebMvcTest(UserControllerEnhanced.class)
@DisplayName("UserControllerEnhanced Integration Tests")
class UserControllerEnhancedTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserServiceRefactored userService;

    private User testUser;
    private UserResponseDTO testUserDTO;
    private LoginRequest loginRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPassword("Password123!");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setRole("USER");

        testUserDTO = new UserResponseDTO();
        testUserDTO.setId("user123");
        testUserDTO.setEmail("test@example.com");
        testUserDTO.setFirstName("John");
        testUserDTO.setLastName("Doe");
        testUserDTO.setRole("USER");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        authResponse = new AuthResponse("jwt-token-123", "test@example.com", "John", "Doe");
    }

    // ==================== Login Endpoint Tests ====================

    @Test
    @DisplayName("POST /api/users/login - Should return JWT token on successful login")
    void login_WithValidCredentials_ShouldReturn200() throws Exception {
        // Arrange
        when(userService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.firstName").value("John"));

        verify(userService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/users/login - Should return 401 on invalid credentials")
    void login_WithInvalidCredentials_ShouldReturn401() throws Exception {
        // Arrange
        when(userService.login(any(LoginRequest.class)))
                .thenThrow(ServiceException.unauthorized("Invalid credentials", "INVALID_CREDENTIALS"));

        // Act & Assert
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());

        verify(userService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/users/login - Should return 400 on missing email")
    void login_WithMissingEmail_ShouldReturn400() throws Exception {
        // Arrange
        loginRequest.setEmail(null);

        // Act & Assert
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).login(any(LoginRequest.class));
    }

    // ==================== Registration Endpoint Tests ====================

    @Test
    @DisplayName("POST /api/users/register - Should create user and return 201")
    void register_WithValidData_ShouldReturn201() throws Exception {
        // Arrange
        when(userService.register(any(User.class))).thenReturn(testUserDTO);

        // Act & Assert
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.message").value("User registered successfully"));

        verify(userService).register(any(User.class));
    }

    @Test
    @DisplayName("POST /api/users/register - Should return 409 on duplicate email")
    void register_WithExistingEmail_ShouldReturn409() throws Exception {
        // Arrange
        when(userService.register(any(User.class)))
                .thenThrow(ServiceException.conflict("Email already exists", "USER_ALREADY_EXISTS"));

        // Act & Assert
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isConflict());

        verify(userService).register(any(User.class));
    }

    // ==================== Get User Endpoint Tests ====================

    @Test
    @DisplayName("GET /api/users/{email} - Should return user when exists")
    void getUserByEmail_WhenExists_ShouldReturn200() throws Exception {
        // Arrange
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUserDTO);

        // Act & Assert
        mockMvc.perform(get("/api/users/test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.firstName").value("John"));

        verify(userService).getUserByEmail("test@example.com");
    }

    @Test
    @DisplayName("GET /api/users/{email} - Should return 404 when not found")
    void getUserByEmail_WhenNotFound_ShouldReturn404() throws Exception {
        // Arrange
        when(userService.getUserByEmail("notfound@example.com"))
                .thenThrow(ServiceException.notFound("User not found", "USER_NOT_FOUND"));

        // Act & Assert
        mockMvc.perform(get("/api/users/notfound@example.com"))
                .andExpect(status().isNotFound());

        verify(userService).getUserByEmail("notfound@example.com");
    }

    // ==================== Update Profile Endpoint Tests ====================

    @Test
    @DisplayName("PUT /api/users/{userId} - Should update profile successfully")
    void updateProfile_WithValidData_ShouldReturn200() throws Exception {
        // Arrange
        User updateData = new User();
        updateData.setFirstName("UpdatedName");

        when(userService.updateUserProfile(eq("user123"), any(User.class))).thenReturn(testUserDTO);

        // Act & Assert
        mockMvc.perform(put("/api/users/user123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile updated successfully"));

        verify(userService).updateUserProfile(eq("user123"), any(User.class));
    }

    // ==================== Delete User Endpoint Tests ====================

    @Test
    @DisplayName("DELETE /api/users/{userId} - Should delete user successfully")
    void deleteUser_WhenExists_ShouldReturn204() throws Exception {
        // Arrange
        doNothing().when(userService).deleteUser("user123");

        // Act & Assert
        mockMvc.perform(delete("/api/users/user123"))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User deleted successfully"));

        verify(userService).deleteUser("user123");
    }

    // ==================== Check Email Availability Tests ====================

    @Test
    @DisplayName("GET /api/users/check-email/{email} - Should return true when available")
    void checkEmailAvailability_WhenAvailable_ShouldReturnTrue() throws Exception {
        // Arrange
        when(userService.existsByEmail("newuser@example.com")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/users/check-email/newuser@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true))
                .andExpect(jsonPath("$.message").value("Email available"));

        verify(userService).existsByEmail("newuser@example.com");
    }

    @Test
    @DisplayName("GET /api/users/check-email/{email} - Should return false when taken")
    void checkEmailAvailability_WhenTaken_ShouldReturnFalse() throws Exception {
        // Arrange
        when(userService.existsByEmail("test@example.com")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/users/check-email/test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(false))
                .andExpect(jsonPath("$.message").value("Email already taken"));

        verify(userService).existsByEmail("test@example.com");
    }

    // ==================== Password Reset Request Tests ====================

    @Test
    @DisplayName("POST /api/users/request-password-reset - Should process request successfully")
    void requestPasswordReset_WithValidEmail_ShouldReturn200() throws Exception {
        // Arrange
        doNothing().when(userService).requestPasswordReset("test@example.com");

        // Act & Assert
        mockMvc.perform(post("/api/users/request-password-reset")
                        .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If the email exists, a password reset link has been sent"));

        verify(userService).requestPasswordReset("test@example.com");
    }
}
