package com.ecommerce.user.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for LoginRequest DTO.
 */
@DisplayName("LoginRequest DTO Tests")
class LoginRequestTest {

    @Test
    @DisplayName("Should set and get email")
    void setEmail_ShouldUpdateEmail() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");

        assertThat(request.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should set and get password")
    void setPassword_ShouldUpdatePassword() {
        LoginRequest request = new LoginRequest();
        request.setPassword("SecurePass123!");

        assertThat(request.getPassword()).isEqualTo("SecurePass123!");
    }

    @Test
    @DisplayName("Default constructor should create empty request")
    void defaultConstructor_ShouldCreateEmptyRequest() {
        LoginRequest request = new LoginRequest();

        assertThat(request.getEmail()).isNull();
        assertThat(request.getPassword()).isNull();
    }

    @Test
    @DisplayName("Should handle null values")
    void setNullValues_ShouldAcceptNulls() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");

        request.setEmail(null);
        request.setPassword(null);

        assertThat(request.getEmail()).isNull();
        assertThat(request.getPassword()).isNull();
    }

    @Test
    @DisplayName("Should create request with both fields")
    void createRequest_WithBothFields_ShouldSetCorrectly() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("MyPassword123!");

        assertThat(request.getEmail()).isEqualTo("user@example.com");
        assertThat(request.getPassword()).isEqualTo("MyPassword123!");
    }

    @Test
    @DisplayName("toString should not expose password")
    void toString_ShouldNotExposePassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("SecretPassword123!");

        String result = request.toString();

        assertThat(result).contains("test@example.com");
        assertThat(result).doesNotContain("SecretPassword123!");
    }

    @Test
    @DisplayName("equals should work correctly")
    void equals_ShouldWorkCorrectly() {
        LoginRequest request1 = new LoginRequest();
        request1.setEmail("test@example.com");
        request1.setPassword("password");

        LoginRequest request2 = new LoginRequest();
        request2.setEmail("test@example.com");
        request2.setPassword("password");

        LoginRequest request3 = new LoginRequest();
        request3.setEmail("different@example.com");
        request3.setPassword("password");

        assertThat(request1).isEqualTo(request2);
        assertThat(request1).isNotEqualTo(request3);
        assertThat(request1).isEqualTo(request1);
        assertThat(request1).isNotEqualTo(null);
    }

    @Test
    @DisplayName("hashCode should be consistent")
    void hashCode_ShouldBeConsistent() {
        LoginRequest request1 = new LoginRequest();
        request1.setEmail("test@example.com");
        request1.setPassword("password");

        LoginRequest request2 = new LoginRequest();
        request2.setEmail("test@example.com");
        request2.setPassword("password");

        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    @DisplayName("Parameterized constructor should set both fields")
    void parameterizedConstructor_ShouldSetBothFields() {
        LoginRequest request = new LoginRequest("test@example.com", "SecurePassword123!");

        assertThat(request.getEmail()).isEqualTo("test@example.com");
        assertThat(request.getPassword()).isEqualTo("SecurePassword123!");
    }

    @Test
    @DisplayName("hasValidCredentials should return true when both fields are present")
    void hasValidCredentials_WhenBothPresent_ShouldReturnTrue() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        assertThat(request.hasValidCredentials()).isTrue();
    }

    @Test
    @DisplayName("hasValidCredentials should return false when email is null")
    void hasValidCredentials_WhenEmailNull_ShouldReturnFalse() {
        LoginRequest request = new LoginRequest(null, "password123");

        assertThat(request.hasValidCredentials()).isFalse();
    }

    @Test
    @DisplayName("hasValidCredentials should return false when email is empty")
    void hasValidCredentials_WhenEmailEmpty_ShouldReturnFalse() {
        LoginRequest request = new LoginRequest("", "password123");

        assertThat(request.hasValidCredentials()).isFalse();
    }

    @Test
    @DisplayName("hasValidCredentials should return false when email is whitespace")
    void hasValidCredentials_WhenEmailWhitespace_ShouldReturnFalse() {
        LoginRequest request = new LoginRequest("   ", "password123");

        assertThat(request.hasValidCredentials()).isFalse();
    }

    @Test
    @DisplayName("hasValidCredentials should return false when password is null")
    void hasValidCredentials_WhenPasswordNull_ShouldReturnFalse() {
        LoginRequest request = new LoginRequest("test@example.com", null);

        assertThat(request.hasValidCredentials()).isFalse();
    }

    @Test
    @DisplayName("hasValidCredentials should return false when password is empty")
    void hasValidCredentials_WhenPasswordEmpty_ShouldReturnFalse() {
        LoginRequest request = new LoginRequest("test@example.com", "");

        assertThat(request.hasValidCredentials()).isFalse();
    }

    @Test
    @DisplayName("equals should return false for different class")
    void equals_WhenDifferentClass_ShouldReturnFalse() {
        LoginRequest request = new LoginRequest("test@example.com", "password");

        assertThat(request).isNotEqualTo("test@example.com");
    }
}
