package com.ecommerce.order.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CartItemDTO.
 */
@DisplayName("CartItemDTO Tests")
class CartItemDTOTest {

    @Test
    @DisplayName("Default constructor should create empty object")
    void defaultConstructor_ShouldCreateEmptyObject() {
        CartItemDTO dto = new CartItemDTO();

        assertThat(dto.getId()).isNull();
        assertThat(dto.getUserEmail()).isNull();
        assertThat(dto.getProductId()).isNull();
        assertThat(dto.getProductName()).isNull();
        assertThat(dto.getProductPrice()).isNull();
        assertThat(dto.getQuantity()).isNull();
    }

    @Test
    @DisplayName("Setters should update all fields correctly")
    void setters_ShouldUpdateAllFields() {
        CartItemDTO dto = new CartItemDTO();

        dto.setId("cart123");
        dto.setUserEmail("user@example.com");
        dto.setProductId("prod456");
        dto.setProductName("Test Product");
        dto.setProductPrice(new BigDecimal("99.99"));
        dto.setQuantity(2);

        assertThat(dto.getId()).isEqualTo("cart123");
        assertThat(dto.getUserEmail()).isEqualTo("user@example.com");
        assertThat(dto.getProductId()).isEqualTo("prod456");
        assertThat(dto.getProductName()).isEqualTo("Test Product");
        assertThat(dto.getProductPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(dto.getQuantity()).isEqualTo(2);
    }
}
