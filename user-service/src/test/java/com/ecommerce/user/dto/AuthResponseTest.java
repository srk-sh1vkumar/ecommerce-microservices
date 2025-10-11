package com.ecommerce.user.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for AuthResponse DTO.
 */
@DisplayName("AuthResponse DTO Tests")
class AuthResponseTest {

    @Test
    @DisplayName("Parameterized constructor should set all fields")
    void constructor_ShouldSetAllFields() {
        AuthResponse response = new AuthResponse("token123", "test@example.com", "John", "Doe");

        assertThat(response.getToken()).isEqualTo("token123");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");
    }

    @Test
    @DisplayName("Should set and get token")
    void setToken_ShouldUpdateToken() {
        AuthResponse response = new AuthResponse();
        response.setToken("newToken456");

        assertThat(response.getToken()).isEqualTo("newToken456");
    }

    @Test
    @DisplayName("Should set and get email")
    void setEmail_ShouldUpdateEmail() {
        AuthResponse response = new AuthResponse();
        response.setEmail("user@example.com");

        assertThat(response.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("Should set and get first name")
    void setFirstName_ShouldUpdateFirstName() {
        AuthResponse response = new AuthResponse();
        response.setFirstName("Jane");

        assertThat(response.getFirstName()).isEqualTo("Jane");
    }

    @Test
    @DisplayName("Should set and get last name")
    void setLastName_ShouldUpdateLastName() {
        AuthResponse response = new AuthResponse();
        response.setLastName("Smith");

        assertThat(response.getLastName()).isEqualTo("Smith");
    }

    @Test
    @DisplayName("Default constructor should create empty response")
    void defaultConstructor_ShouldCreateEmptyResponse() {
        AuthResponse response = new AuthResponse();

        assertThat(response.getToken()).isNull();
        assertThat(response.getEmail()).isNull();
        assertThat(response.getFirstName()).isNull();
        assertThat(response.getLastName()).isNull();
    }

    @Test
    @DisplayName("Should handle null values")
    void setNullValues_ShouldAcceptNulls() {
        AuthResponse response = new AuthResponse("token", "email", "first", "last");

        response.setToken(null);
        response.setEmail(null);
        response.setFirstName(null);
        response.setLastName(null);

        assertThat(response.getToken()).isNull();
        assertThat(response.getEmail()).isNull();
        assertThat(response.getFirstName()).isNull();
        assertThat(response.getLastName()).isNull();
    }

    @Test
    @DisplayName("toString should include user fields")
    void toString_ShouldIncludeUserFields() {
        AuthResponse response = new AuthResponse("token123", "test@example.com", "John", "Doe");

        String result = response.toString();

        assertThat(result).contains("test@example.com");
        assertThat(result).contains("John");
        assertThat(result).contains("Doe");
        // Token may be excluded from toString for security
    }

    @Test
    @DisplayName("equals should work correctly")
    void equals_ShouldWorkCorrectly() {
        AuthResponse response1 = new AuthResponse("token", "email@test.com", "John", "Doe");
        AuthResponse response2 = new AuthResponse("token", "email@test.com", "John", "Doe");
        AuthResponse response3 = new AuthResponse("different", "email@test.com", "John", "Doe");

        assertThat(response1).isEqualTo(response2);
        assertThat(response1).isNotEqualTo(response3);
        assertThat(response1).isEqualTo(response1);
        assertThat(response1).isNotEqualTo(null);
    }

    @Test
    @DisplayName("hashCode should be consistent")
    void hashCode_ShouldBeConsistent() {
        AuthResponse response1 = new AuthResponse("token", "email@test.com", "John", "Doe");
        AuthResponse response2 = new AuthResponse("token", "email@test.com", "John", "Doe");

        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    @DisplayName("Six-parameter constructor should set all fields")
    void sixParamConstructor_ShouldSetAllFields() {
        AuthResponse response = new AuthResponse("token123", "test@example.com", "John", "Doe", "USER", 3600);

        assertThat(response.getToken()).isEqualTo("token123");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");
        assertThat(response.getRole()).isEqualTo("USER");
        assertThat(response.getExpiresIn()).isEqualTo(3600);
    }

    @Test
    @DisplayName("Should set and get role")
    void setRole_ShouldUpdateRole() {
        AuthResponse response = new AuthResponse();
        response.setRole("ADMIN");

        assertThat(response.getRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Should set and get expiresIn")
    void setExpiresIn_ShouldUpdateExpiresIn() {
        AuthResponse response = new AuthResponse();
        response.setExpiresIn(7200);

        assertThat(response.getExpiresIn()).isEqualTo(7200);
    }

    @Test
    @DisplayName("Should set and get tokenType")
    void setTokenType_ShouldUpdateTokenType() {
        AuthResponse response = new AuthResponse();
        response.setTokenType("Custom");

        assertThat(response.getTokenType()).isEqualTo("Custom");
    }

    @Test
    @DisplayName("Default tokenType should be Bearer")
    void defaultTokenType_ShouldBeBearer() {
        AuthResponse response = new AuthResponse();

        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("Default constructor should set authenticatedAt")
    void defaultConstructor_ShouldSetAuthenticatedAt() {
        AuthResponse response = new AuthResponse();

        assertThat(response.getAuthenticatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should set and get authenticatedAt")
    void setAuthenticatedAt_ShouldUpdateAuthenticatedAt() {
        AuthResponse response = new AuthResponse();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        response.setAuthenticatedAt(now);

        assertThat(response.getAuthenticatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("getFullName should return first and last name")
    void getFullName_WithBothNames_ShouldReturnFullName() {
        AuthResponse response = new AuthResponse("token", "test@example.com", "John", "Doe");

        assertThat(response.getFullName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("getFullName should return only first name when last is null")
    void getFullName_WithOnlyFirstName_ShouldReturnFirstName() {
        AuthResponse response = new AuthResponse();
        response.setFirstName("John");
        response.setLastName(null);

        assertThat(response.getFullName()).isEqualTo("John");
    }

    @Test
    @DisplayName("getFullName should return only last name when first is null")
    void getFullName_WithOnlyLastName_ShouldReturnLastName() {
        AuthResponse response = new AuthResponse();
        response.setFirstName(null);
        response.setLastName("Doe");

        assertThat(response.getFullName()).isEqualTo("Doe");
    }

    @Test
    @DisplayName("getFullName should return null when both names are null")
    void getFullName_WithNoNames_ShouldReturnNull() {
        AuthResponse response = new AuthResponse();
        response.setFirstName(null);
        response.setLastName(null);

        assertThat(response.getFullName()).isNull();
    }

    @Test
    @DisplayName("isValid should return true when token and email are present")
    void isValid_WithTokenAndEmail_ShouldReturnTrue() {
        AuthResponse response = new AuthResponse("token123", "test@example.com", "John", "Doe");

        assertThat(response.isValid()).isTrue();
    }

    @Test
    @DisplayName("isValid should return false when token is null")
    void isValid_WithNullToken_ShouldReturnFalse() {
        AuthResponse response = new AuthResponse();
        response.setToken(null);
        response.setEmail("test@example.com");

        assertThat(response.isValid()).isFalse();
    }

    @Test
    @DisplayName("isValid should return false when token is empty")
    void isValid_WithEmptyToken_ShouldReturnFalse() {
        AuthResponse response = new AuthResponse();
        response.setToken("");
        response.setEmail("test@example.com");

        assertThat(response.isValid()).isFalse();
    }

    @Test
    @DisplayName("isValid should return false when token is whitespace")
    void isValid_WithWhitespaceToken_ShouldReturnFalse() {
        AuthResponse response = new AuthResponse();
        response.setToken("   ");
        response.setEmail("test@example.com");

        assertThat(response.isValid()).isFalse();
    }

    @Test
    @DisplayName("isValid should return false when email is null")
    void isValid_WithNullEmail_ShouldReturnFalse() {
        AuthResponse response = new AuthResponse();
        response.setToken("token123");
        response.setEmail(null);

        assertThat(response.isValid()).isFalse();
    }

    @Test
    @DisplayName("isValid should return false when email is empty")
    void isValid_WithEmptyEmail_ShouldReturnFalse() {
        AuthResponse response = new AuthResponse();
        response.setToken("token123");
        response.setEmail("");

        assertThat(response.isValid()).isFalse();
    }

    @Test
    @DisplayName("isValid should return false when email is whitespace")
    void isValid_WithWhitespaceEmail_ShouldReturnFalse() {
        AuthResponse response = new AuthResponse();
        response.setToken("token123");
        response.setEmail("   ");

        assertThat(response.isValid()).isFalse();
    }

    @Test
    @DisplayName("equals should return false for different class")
    void equals_WhenDifferentClass_ShouldReturnFalse() {
        AuthResponse response = new AuthResponse("token", "email@test.com", "John", "Doe");

        assertThat(response).isNotEqualTo("some string");
    }
}
