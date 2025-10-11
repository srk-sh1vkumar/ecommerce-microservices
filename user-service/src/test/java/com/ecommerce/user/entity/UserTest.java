package com.ecommerce.user.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for User entity.
 * Tests entity construction, getters, setters, equals, hashCode, and toString.
 */
@DisplayName("User Entity Tests")
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
    }

    @Test
    @DisplayName("Should set and get id")
    void setId_ShouldUpdateId() {
        user.setId("user123");
        assertThat(user.getId()).isEqualTo("user123");
    }

    @Test
    @DisplayName("Should set and get email")
    void setEmail_ShouldUpdateEmail() {
        user.setEmail("test@example.com");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should set and get password")
    void setPassword_ShouldUpdatePassword() {
        user.setPassword("SecurePass123!");
        assertThat(user.getPassword()).isEqualTo("SecurePass123!");
    }

    @Test
    @DisplayName("Should set and get first name")
    void setFirstName_ShouldUpdateFirstName() {
        user.setFirstName("John");
        assertThat(user.getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should set and get last name")
    void setLastName_ShouldUpdateLastName() {
        user.setLastName("Doe");
        assertThat(user.getLastName()).isEqualTo("Doe");
    }

    @Test
    @DisplayName("Should set and get role")
    void setRole_ShouldUpdateRole() {
        user.setRole("ADMIN");
        assertThat(user.getRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Should set and get created at")
    void setCreatedAt_ShouldUpdateCreatedAt() {
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        assertThat(user.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should create user with all fields")
    void createUser_WithAllFields_ShouldSetCorrectly() {
        LocalDateTime now = LocalDateTime.now();

        user.setId("user456");
        user.setEmail("john.doe@example.com");
        user.setPassword("Password123!");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setRole("USER");
        user.setCreatedAt(now);

        assertThat(user.getId()).isEqualTo("user456");
        assertThat(user.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(user.getPassword()).isEqualTo("Password123!");
        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.getRole()).isEqualTo("USER");
        assertThat(user.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should handle null values")
    void setNullValues_ShouldAcceptNulls() {
        user.setId(null);
        user.setEmail(null);
        user.setFirstName(null);

        assertThat(user.getId()).isNull();
        assertThat(user.getEmail()).isNull();
        assertThat(user.getFirstName()).isNull();
    }

    @Test
    @DisplayName("Two users with same fields should be equal")
    void equals_WithSameFields_ShouldReturnTrue() {
        User user1 = new User();
        user1.setId("user123");
        user1.setEmail("test@example.com");
        user1.setFirstName("John");

        User user2 = new User();
        user2.setId("user123");
        user2.setEmail("test@example.com");
        user2.setFirstName("John");

        assertThat(user1).isEqualTo(user2);
    }

    @Test
    @DisplayName("Two users with different fields should not be equal")
    void equals_WithDifferentFields_ShouldReturnFalse() {
        User user1 = new User();
        user1.setId("user123");
        user1.setEmail("test1@example.com");

        User user2 = new User();
        user2.setId("user456");
        user2.setEmail("test2@example.com");

        assertThat(user1).isNotEqualTo(user2);
    }

    @Test
    @DisplayName("User should equal itself")
    void equals_WithSameObject_ShouldReturnTrue() {
        user.setId("user123");
        assertThat(user).isEqualTo(user);
    }

    @Test
    @DisplayName("User should not equal null")
    void equals_WithNull_ShouldReturnFalse() {
        user.setId("user123");
        assertThat(user).isNotEqualTo(null);
    }

    @Test
    @DisplayName("User should not equal different class")
    void equals_WithDifferentClass_ShouldReturnFalse() {
        user.setId("user123");
        assertThat(user).isNotEqualTo("not a user");
    }

    @Test
    @DisplayName("Two users with same id should have same hashCode")
    void hashCode_WithSameId_ShouldBeEqual() {
        User user1 = new User();
        user1.setId("user123");

        User user2 = new User();
        user2.setId("user123");

        assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
    }

    @Test
    @DisplayName("toString should include key fields")
    void toString_ShouldIncludeKeyFields() {
        user.setId("user123");
        user.setEmail("test@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setRole("USER");

        String result = user.toString();

        assertThat(result).contains("user123");
        assertThat(result).contains("test@example.com");
        assertThat(result).contains("John");
        assertThat(result).contains("Doe");
        assertThat(result).contains("USER");
        assertThat(result).doesNotContain("Password"); // Password should not be in toString
    }
}
