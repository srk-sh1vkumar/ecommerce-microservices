package com.ecommerce.user.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for UserResponseDTO.
 */
@DisplayName("UserResponseDTO Tests")
class UserResponseDTOTest {

    @Test
    @DisplayName("Should set and get all fields")
    void setAllFields_ShouldWork() {
        UserResponseDTO dto = new UserResponseDTO();

        dto.setId("user123");
        dto.setEmail("test@example.com");
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setRole("USER");

        assertThat(dto.getId()).isEqualTo("user123");
        assertThat(dto.getEmail()).isEqualTo("test@example.com");
        assertThat(dto.getFirstName()).isEqualTo("John");
        assertThat(dto.getLastName()).isEqualTo("Doe");
        assertThat(dto.getRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("Default constructor should create empty DTO")
    void defaultConstructor_ShouldCreateEmpty() {
        UserResponseDTO dto = new UserResponseDTO();

        assertThat(dto.getId()).isNull();
        assertThat(dto.getEmail()).isNull();
        assertThat(dto.getFirstName()).isNull();
        assertThat(dto.getLastName()).isNull();
        assertThat(dto.getRole()).isNull();
    }

    @Test
    @DisplayName("Should handle null values")
    void setNullValues_ShouldWork() {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId("user123");
        dto.setEmail("test@example.com");

        dto.setId(null);
        dto.setEmail(null);

        assertThat(dto.getId()).isNull();
        assertThat(dto.getEmail()).isNull();
    }

}
