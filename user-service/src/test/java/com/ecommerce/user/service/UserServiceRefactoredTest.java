package com.ecommerce.user.service;

import com.ecommerce.common.exception.ServiceException;
import com.ecommerce.common.metrics.MetricsService;
import com.ecommerce.common.util.JwtUtil;
import com.ecommerce.user.client.NotificationServiceClient;
import com.ecommerce.user.dto.AuthResponse;
import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.dto.UserResponseDTO;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.mapper.UserMapper;
import com.ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for UserServiceRefactored.
 * Tests business logic, validation, error handling, and edge cases.
 *
 * @author E-commerce Development Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceRefactored Unit Tests")
class UserServiceRefactoredTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserMapper userMapper;

    @Mock
    private MetricsService metricsService;

    @Mock
    private NotificationServiceClient notificationServiceClient;

    @InjectMocks
    private UserServiceRefactored userService;

    private User testUser;
    private UserResponseDTO testUserDTO;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // Set base URL for email links
        ReflectionTestUtils.setField(userService, "baseUrl", "http://localhost:8080");

        // Setup test data
        testUser = new User();
        testUser.setId("user123");
        testUser.setEmail("test@example.com");
        testUser.setPassword("$2a$10$hashedPassword");
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
    }

    // ==================== Login Tests ====================

    @Test
    @DisplayName("Login - Should successfully authenticate user with valid credentials")
    void login_WithValidCredentials_ShouldReturnAuthResponse() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateTokenWithRole(testUser.getEmail(), testUser.getRole()))
                .thenReturn("jwt-token-123");

        // Act
        AuthResponse response = userService.login(loginRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token-123");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");

        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("password123", testUser.getPassword());
        verify(jwtUtil).generateTokenWithRole(testUser.getEmail(), testUser.getRole());
        verify(metricsService).incrementUserLogins();
    }

    @Test
    @DisplayName("Login - Should fail with non-existent email")
    void login_WithNonExistentEmail_ShouldThrowException() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Invalid credentials");

        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(metricsService, never()).incrementUserLogins();
    }

    @Test
    @DisplayName("Login - Should fail with incorrect password")
    void login_WithIncorrectPassword_ShouldThrowException() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPassword())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Invalid credentials");

        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("password123", testUser.getPassword());
        verify(metricsService).incrementFailedLogins();
        verify(metricsService, never()).incrementUserLogins();
    }

    @Test
    @DisplayName("Login - Should normalize email to lowercase")
    void login_ShouldNormalizeEmail() {
        // Arrange
        loginRequest.setEmail("Test@Example.COM");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateTokenWithRole(anyString(), anyString())).thenReturn("token");

        // Act
        userService.login(loginRequest);

        // Assert
        verify(userRepository).findByEmail("test@example.com");
    }

    // ==================== Registration Tests ====================

    @Test
    @DisplayName("Register - Should successfully create new user")
    void register_WithValidData_ShouldCreateUser() {
        // Arrange
        User newUser = new User();
        newUser.setEmail("newuser@example.com");
        newUser.setPassword("StrongPass123!");
        newUser.setFirstName("Jane");
        newUser.setLastName("Smith");

        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPass123!")).thenReturn("$2a$10$hashedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(userMapper.toResponseDTO(any(User.class))).thenReturn(testUserDTO);
        when(jwtUtil.generateToken(anyString())).thenReturn("activation-token");

        // Act
        UserResponseDTO result = userService.register(newUser);

        // Assert
        assertThat(result).isNotNull();
        verify(userRepository).existsByEmail("newuser@example.com");
        verify(passwordEncoder).encode("StrongPass123!");
        verify(userRepository).save(argThat(user ->
                user.getEmail().equals("newuser@example.com") &&
                user.getPassword().equals("$2a$10$hashedNewPassword") &&
                user.getRole().equals("USER")
        ));
        verify(metricsService).incrementUserRegistrations();
        verify(userMapper).toResponseDTO(any(User.class));
    }

    @Test
    @DisplayName("Register - Should fail when email already exists")
    void register_WithExistingEmail_ShouldThrowException() {
        // Arrange
        User newUser = new User();
        newUser.setEmail("test@example.com");
        newUser.setPassword("password123");
        newUser.setFirstName("Jane");
        newUser.setLastName("Smith");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.register(newUser))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("already exists");

        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
        verify(metricsService, never()).incrementUserRegistrations();
    }

    @Test
    @DisplayName("Register - Should set default role when not specified")
    void register_WithoutRole_ShouldSetDefaultRole() {
        // Arrange
        User newUser = new User();
        newUser.setEmail("newuser@example.com");
        newUser.setPassword("password123");
        newUser.setFirstName("Jane");
        newUser.setLastName("Smith");
        // role is null

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(userMapper.toResponseDTO(any(User.class))).thenReturn(testUserDTO);
        when(jwtUtil.generateToken(anyString())).thenReturn("token");

        // Act
        userService.register(newUser);

        // Assert
        verify(userRepository).save(argThat(user -> user.getRole().equals("USER")));
    }

    @Test
    @DisplayName("Register - Should normalize email to lowercase")
    void register_ShouldNormalizeEmail() {
        // Arrange
        User newUser = new User();
        newUser.setEmail("NewUser@Example.COM");
        newUser.setPassword("password123");
        newUser.setFirstName("Jane");
        newUser.setLastName("Smith");

        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(userMapper.toResponseDTO(any(User.class))).thenReturn(testUserDTO);
        when(jwtUtil.generateToken(anyString())).thenReturn("token");

        // Act
        userService.register(newUser);

        // Assert
        verify(userRepository).existsByEmail("newuser@example.com");
        assertThat(newUser.getEmail()).isEqualTo("newuser@example.com");
    }

    // ==================== Get User By Email Tests ====================

    @Test
    @DisplayName("GetUserByEmail - Should return user when exists")
    void getUserByEmail_WhenUserExists_ShouldReturnUser() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userMapper.toResponseDTO(testUser)).thenReturn(testUserDTO);

        // Act
        UserResponseDTO result = userService.getUserByEmail("test@example.com");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(userRepository).findByEmail("test@example.com");
        verify(userMapper).toResponseDTO(testUser);
    }

    @Test
    @DisplayName("GetUserByEmail - Should throw exception when user not found")
    void getUserByEmail_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserByEmail("notfound@example.com"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("not found");

        verify(userRepository).findByEmail("notfound@example.com");
        verify(userMapper, never()).toResponseDTO(any());
    }

    // ==================== Update User Profile Tests ====================

    @Test
    @DisplayName("UpdateUserProfile - Should update allowed fields")
    void updateUserProfile_WithValidData_ShouldUpdateFields() {
        // Arrange
        User updateData = new User();
        updateData.setFirstName("UpdatedName");
        updateData.setLastName("UpdatedLastName");

        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponseDTO(any(User.class))).thenReturn(testUserDTO);

        // Act
        UserResponseDTO result = userService.updateUserProfile("user123", updateData);

        // Assert
        assertThat(result).isNotNull();
        verify(userRepository).findById("user123");
        verify(userRepository).save(argThat(user ->
                user.getFirstName().equals("UpdatedName") &&
                user.getLastName().equals("UpdatedLastName")
        ));
    }

    @Test
    @DisplayName("UpdateUserProfile - Should fail when updating to existing email")
    void updateUserProfile_WithExistingEmail_ShouldThrowException() {
        // Arrange
        User updateData = new User();
        updateData.setEmail("another@example.com");

        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("another@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUserProfile("user123", updateData))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("already in use");

        verify(userRepository).findById("user123");
        verify(userRepository).existsByEmail("another@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("UpdateUserProfile - Should allow updating to same email")
    void updateUserProfile_WithSameEmail_ShouldNotCheckDuplicate() {
        // Arrange
        User updateData = new User();
        updateData.setEmail("test@example.com"); // Same as current email
        updateData.setFirstName("UpdatedName");

        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponseDTO(any(User.class))).thenReturn(testUserDTO);

        // Act
        userService.updateUserProfile("user123", updateData);

        // Assert
        verify(userRepository).findById("user123");
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository).save(any(User.class));
    }

    // ==================== Delete User Tests ====================

    @Test
    @DisplayName("DeleteUser - Should delete existing user")
    void deleteUser_WhenUserExists_ShouldDelete() {
        // Arrange
        when(userRepository.existsById("user123")).thenReturn(true);

        // Act
        userService.deleteUser("user123");

        // Assert
        verify(userRepository).existsById("user123");
        verify(userRepository).deleteById("user123");
    }

    @Test
    @DisplayName("DeleteUser - Should throw exception when user not found")
    void deleteUser_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        when(userRepository.existsById("nonexistent")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteUser("nonexistent"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("not found");

        verify(userRepository).existsById("nonexistent");
        verify(userRepository, never()).deleteById(anyString());
    }

    // ==================== Exists By Email Tests ====================

    @Test
    @DisplayName("ExistsByEmail - Should return true when email exists")
    void existsByEmail_WhenExists_ShouldReturnTrue() {
        // Arrange
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // Act
        boolean result = userService.existsByEmail("test@example.com");

        // Assert
        assertThat(result).isTrue();
        verify(userRepository).existsByEmail("test@example.com");
    }

    @Test
    @DisplayName("ExistsByEmail - Should return false when email doesn't exist")
    void existsByEmail_WhenNotExists_ShouldReturnFalse() {
        // Arrange
        when(userRepository.existsByEmail("notfound@example.com")).thenReturn(false);

        // Act
        boolean result = userService.existsByEmail("notfound@example.com");

        // Assert
        assertThat(result).isFalse();
        verify(userRepository).existsByEmail("notfound@example.com");
    }

    // ==================== Password Reset Tests ====================

    @Test
    @DisplayName("RequestPasswordReset - Should process request for existing user")
    void requestPasswordReset_WhenUserExists_ShouldProcess() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(anyString())).thenReturn("reset-token");

        // Act
        userService.requestPasswordReset("test@example.com");

        // Assert
        verify(userRepository).findByEmail("test@example.com");
        // Note: Async method call to notification service won't be verified in unit test
    }

    @Test
    @DisplayName("RequestPasswordReset - Should throw exception for non-existent user")
    void requestPasswordReset_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.requestPasswordReset("notfound@example.com"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("not found");

        verify(userRepository).findByEmail("notfound@example.com");
    }
}
