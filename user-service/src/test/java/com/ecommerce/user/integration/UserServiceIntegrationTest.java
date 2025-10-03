package com.ecommerce.user.integration;

import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.dto.UserResponseDTO;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.service.UserServiceRefactored;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for User Service using Testcontainers.
 * Tests the full stack with real MongoDB database.
 *
 * @author E-commerce Development Team
 * @version 1.0
 */
@SpringBootTest
@Testcontainers
@DisplayName("User Service Integration Tests with Testcontainers")
class UserServiceIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
        // Disable Eureka for tests
        registry.add("eureka.client.enabled", () -> "false");
        // Disable async for predictable testing
        registry.add("spring.task.execution.pool.core-size", () -> "0");
    }

    @Autowired
    private UserServiceRefactored userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Integration - Should register, login, and retrieve user successfully")
    void fullUserLifecycle_ShouldWorkEndToEnd() {
        // Step 1: Register a new user
        User newUser = new User();
        newUser.setEmail("integration@example.com");
        newUser.setPassword("Password123!");
        newUser.setFirstName("Integration");
        newUser.setLastName("Test");

        UserResponseDTO registeredUser = userService.register(newUser);

        assertThat(registeredUser).isNotNull();
        assertThat(registeredUser.getEmail()).isEqualTo("integration@example.com");
        assertThat(registeredUser.getFirstName()).isEqualTo("Integration");
        assertThat(registeredUser.getRole()).isEqualTo("USER");

        // Step 2: Verify user exists in database
        boolean exists = userService.existsByEmail("integration@example.com");
        assertThat(exists).isTrue();

        // Step 3: Login with the registered user
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("integration@example.com");
        loginRequest.setPassword("Password123!");

        var authResponse = userService.login(loginRequest);

        assertThat(authResponse).isNotNull();
        assertThat(authResponse.getToken()).isNotEmpty();
        assertThat(authResponse.getEmail()).isEqualTo("integration@example.com");

        // Step 4: Retrieve user by email
        UserResponseDTO retrievedUser = userService.getUserByEmail("integration@example.com");

        assertThat(retrievedUser).isNotNull();
        assertThat(retrievedUser.getEmail()).isEqualTo("integration@example.com");
        assertThat(retrievedUser.getFirstName()).isEqualTo("Integration");
    }

    @Test
    @DisplayName("Integration - Should update user profile correctly")
    void updateUserProfile_ShouldPersistChanges() {
        // Arrange: Create a user
        User newUser = new User();
        newUser.setEmail("update@example.com");
        newUser.setPassword("Password123!");
        newUser.setFirstName("Original");
        newUser.setLastName("Name");

        UserResponseDTO registered = userService.register(newUser);
        String userId = registered.getId();

        // Act: Update the user
        User updateData = new User();
        updateData.setFirstName("Updated");
        updateData.setLastName("LastName");

        UserResponseDTO updated = userService.updateUserProfile(userId, updateData);

        // Assert: Verify changes persisted
        assertThat(updated.getFirstName()).isEqualTo("Updated");
        assertThat(updated.getLastName()).isEqualTo("LastName");
        assertThat(updated.getEmail()).isEqualTo("update@example.com"); // Unchanged

        // Verify in database
        UserResponseDTO retrieved = userService.getUserByEmail("update@example.com");
        assertThat(retrieved.getFirstName()).isEqualTo("Updated");
        assertThat(retrieved.getLastName()).isEqualTo("LastName");
    }

    @Test
    @DisplayName("Integration - Should handle duplicate email registration correctly")
    void register_WithDuplicateEmail_ShouldFail() {
        // Arrange: Register first user
        User firstUser = new User();
        firstUser.setEmail("duplicate@example.com");
        firstUser.setPassword("Password123!");
        firstUser.setFirstName("First");
        firstUser.setLastName("User");

        userService.register(firstUser);

        // Act & Assert: Try to register second user with same email
        User secondUser = new User();
        secondUser.setEmail("duplicate@example.com");
        secondUser.setPassword("DifferentPass123!");
        secondUser.setFirstName("Second");
        secondUser.setLastName("User");

        assertThatThrownBy(() -> userService.register(secondUser))
                .hasMessageContaining("already exists");

        // Verify only one user exists
        long count = userRepository.count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Integration - Should delete user and verify removal")
    void deleteUser_ShouldRemoveFromDatabase() {
        // Arrange: Create a user
        User newUser = new User();
        newUser.setEmail("delete@example.com");
        newUser.setPassword("Password123!");
        newUser.setFirstName("Delete");
        newUser.setLastName("Me");

        UserResponseDTO registered = userService.register(newUser);
        String userId = registered.getId();

        // Verify user exists
        assertThat(userService.existsByEmail("delete@example.com")).isTrue();

        // Act: Delete the user
        userService.deleteUser(userId);

        // Assert: Verify user is deleted
        assertThat(userService.existsByEmail("delete@example.com")).isFalse();
        assertThat(userRepository.existsById(userId)).isFalse();
    }

    @Test
    @DisplayName("Integration - Should normalize email to lowercase")
    void register_WithMixedCaseEmail_ShouldNormalize() {
        // Arrange
        User newUser = new User();
        newUser.setEmail("MixedCase@Example.COM");
        newUser.setPassword("Password123!");
        newUser.setFirstName("Test");
        newUser.setLastName("User");

        // Act
        UserResponseDTO registered = userService.register(newUser);

        // Assert
        assertThat(registered.getEmail()).isEqualTo("mixedcase@example.com");

        // Verify can login with normalized email
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("mixedcase@example.com");
        loginRequest.setPassword("Password123!");

        var authResponse = userService.login(loginRequest);
        assertThat(authResponse).isNotNull();
    }
}
