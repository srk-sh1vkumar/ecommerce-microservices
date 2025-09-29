package com.ecommerce.user.service;

import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.dto.AuthResponse;
import com.ecommerce.user.util.JwtUtil;
import com.ecommerce.user.constants.SecurityConstants;
import com.ecommerce.user.exception.UserServiceException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for UserService.
 * 
 * This test class validates all business logic in the UserService including:
 * - User registration with various scenarios
 * - User authentication and login
 * - User profile management
 * - Input validation and error handling
 * - Security measures and password handling
 * 
 * Testing Strategy:
 * - Unit tests with mocked dependencies
 * - Positive and negative test cases
 * - Edge cases and boundary conditions
 * - Security validation tests
 * - Exception handling verification
 * 
 * Test Coverage:
 * - Happy path scenarios
 * - Error conditions
 * - Input validation
 * - Security constraints
 * - Business rule enforcement
 * 
 * @author Ecommerce Development Team
 * @version 1.0
 * @since 2024-01-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("User Service Tests")
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtUtil jwtUtil;
    
    @InjectMocks
    private UserService userService;
    
    private User testUser;
    private LoginRequest loginRequest;
    
    /**
     * Set up test data before each test method.
     * Creates consistent test objects to ensure reliable testing.
     */
    @BeforeEach
    void setUp() {
        // Create a test user with valid data
        testUser = new User();
        testUser.setId("64a1b2c3d4e5f6789abc0123");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedPassword123");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setRole(SecurityConstants.DEFAULT_USER_ROLE);
        testUser.setCreatedAt(LocalDateTime.now());
        
        // Create a test login request
        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("Password123!");
    }
    
    /**
     * Test successful user registration with valid data.
     * Verifies that a new user can be registered with proper validation and security.
     */
    @Test
    @DisplayName("Should register user successfully with valid data")
    void testRegisterUser_Success() {
        // Arrange
        User newUser = new User("new@example.com", "Password123!", "Jane", "Smith");
        
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        
        // Act
        User result = userService.register(newUser);
        
        // Assert
        assertNotNull(result);
        assertEquals("new@example.com", result.getEmail());
        assertEquals("Jane", result.getFirstName());
        assertEquals("Smith", result.getLastName());
        assertEquals(SecurityConstants.DEFAULT_USER_ROLE, result.getRole());
        assertNull(result.getPassword()); // Password should be removed from response
        
        // Verify interactions
        verify(userRepository).existsByEmail("new@example.com");
        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(any(User.class));
    }
    
    /**
     * Test user registration failure when email already exists.
     * Verifies that the system prevents duplicate email registrations.
     */
    @Test
    @DisplayName("Should throw exception when registering with existing email")
    void testRegisterUser_EmailAlreadyExists() {
        // Arrange
        User newUser = new User("existing@example.com", "Password123!", "Jane", "Smith");
        
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);
        
        // Act & Assert
        UserServiceException exception = assertThrows(UserServiceException.class, 
            () -> userService.register(newUser));
        
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals(SecurityConstants.USER_ALREADY_EXISTS, exception.getMessage());
        
        // Verify that save was not called
        verify(userRepository, never()).save(any(User.class));
    }
    
    /**
     * Test user registration failure with weak password.
     * Verifies that password strength validation is enforced.
     */
    @Test
    @DisplayName("Should throw exception when registering with weak password")
    void testRegisterUser_WeakPassword() {
        // Arrange
        User newUser = new User("new@example.com", "weakpass", "Jane", "Smith");
        
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        
        // Act & Assert
        UserServiceException exception = assertThrows(UserServiceException.class, 
            () -> userService.register(newUser));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("uppercase letter"));
        
        // Verify that save was not called
        verify(userRepository, never()).save(any(User.class));
    }
    
    /**
     * Test successful user login with valid credentials.
     * Verifies authentication process and JWT token generation.
     */
    @Test
    @DisplayName("Should login user successfully with valid credentials")
    void testLogin_Success() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password123!", "hashedPassword123")).thenReturn(true);
        when(jwtUtil.generateToken("test@example.com")).thenReturn("jwt-token-123");
        
        // Act
        AuthResponse result = userService.login(loginRequest);
        
        // Assert
        assertNotNull(result);
        assertEquals("jwt-token-123", result.getToken());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        
        // Verify interactions
        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("Password123!", "hashedPassword123");
        verify(jwtUtil).generateToken("test@example.com");
    }
    
    /**
     * Test login failure with non-existent user.
     * Verifies proper error handling for invalid users.
     */
    @Test
    @DisplayName("Should throw exception when user not found during login")
    void testLogin_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        
        LoginRequest invalidRequest = new LoginRequest();
        invalidRequest.setEmail("nonexistent@example.com");
        invalidRequest.setPassword("Password123!");
        
        // Act & Assert
        UserServiceException exception = assertThrows(UserServiceException.class, 
            () -> userService.login(invalidRequest));
        
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals(SecurityConstants.INVALID_CREDENTIALS, exception.getMessage());
        
        // Verify that password encoding was not attempted
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
    
    /**
     * Test login failure with incorrect password.
     * Verifies password validation and security measures.
     */
    @Test
    @DisplayName("Should throw exception when password is incorrect")
    void testLogin_IncorrectPassword() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPassword!", "hashedPassword123")).thenReturn(false);
        
        LoginRequest invalidRequest = new LoginRequest();
        invalidRequest.setEmail("test@example.com");
        invalidRequest.setPassword("WrongPassword!");
        
        // Act & Assert
        UserServiceException exception = assertThrows(UserServiceException.class, 
            () -> userService.login(invalidRequest));
        
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals(SecurityConstants.INVALID_CREDENTIALS, exception.getMessage());
        
        // Verify that token generation was not attempted
        verify(jwtUtil, never()).generateToken(anyString());
    }
    
    /**
     * Test successful user retrieval by email.
     * Verifies that users can be found and returned with proper security.
     */
    @Test
    @DisplayName("Should retrieve user by email successfully")
    void testGetUserByEmail_Success() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        
        // Act
        User result = userService.getUserByEmail("test@example.com");
        
        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertNull(result.getPassword()); // Password should be removed for security
        
        verify(userRepository).findByEmail("test@example.com");
    }
    
    /**
     * Test user retrieval failure when user not found.
     * Verifies proper error handling for non-existent users.
     */
    @Test
    @DisplayName("Should throw exception when user not found by email")
    void testGetUserByEmail_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());
        
        // Act & Assert
        UserServiceException exception = assertThrows(UserServiceException.class, 
            () -> userService.getUserByEmail("notfound@example.com"));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals(SecurityConstants.USER_NOT_FOUND, exception.getMessage());
    }
    
    /**
     * Test email normalization during registration.
     * Verifies that emails are properly normalized (lowercase, trimmed).
     */
    @Test
    @DisplayName("Should normalize email during registration")
    void testRegisterUser_EmailNormalization() {
        // Arrange
        User newUser = new User("  NEW@EXAMPLE.COM  ", "Password123!", "Jane", "Smith");
        
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("generated-id");
            return user;
        });
        
        // Act
        User result = userService.register(newUser);
        
        // Assert
        assertEquals("new@example.com", result.getEmail());
        
        // Verify that the normalized email was used for existence check
        verify(userRepository).existsByEmail("new@example.com");
    }
    
    /**
     * Test input validation for null login request.
     * Verifies proper handling of invalid input data.
     */
    @Test
    @DisplayName("Should throw exception for null login request")
    void testLogin_NullRequest() {
        // Act & Assert
        UserServiceException exception = assertThrows(UserServiceException.class, 
            () -> userService.login(null));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Email and password are required", exception.getMessage());
    }
    
    /**
     * Test input validation for empty email in login request.
     * Verifies proper handling of missing required fields.
     */
    @Test
    @DisplayName("Should throw exception for empty email in login")
    void testLogin_EmptyEmail() {
        // Arrange
        LoginRequest emptyEmailRequest = new LoginRequest();
        emptyEmailRequest.setEmail("");
        emptyEmailRequest.setPassword("Password123!");
        
        // Act & Assert
        UserServiceException exception = assertThrows(UserServiceException.class, 
            () -> userService.login(emptyEmailRequest));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Email and password are required", exception.getMessage());
    }
}