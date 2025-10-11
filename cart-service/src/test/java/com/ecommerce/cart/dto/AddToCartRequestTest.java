package com.ecommerce.cart.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for AddToCartRequest DTO.
 */
@DisplayName("AddToCartRequest DTO Tests")
class AddToCartRequestTest {

    @Test
    @DisplayName("Default constructor should create empty request")
    void defaultConstructor_ShouldCreateEmptyRequest() {
        AddToCartRequest request = new AddToCartRequest();

        assertThat(request.getUserEmail()).isNull();
        assertThat(request.getProductId()).isNull();
        assertThat(request.getQuantity()).isNull();
    }

    @Test
    @DisplayName("Parameterized constructor should set all fields")
    void parameterizedConstructor_ShouldSetAllFields() {
        AddToCartRequest request = new AddToCartRequest("user@example.com", "product123", 5);

        assertThat(request.getUserEmail()).isEqualTo("user@example.com");
        assertThat(request.getProductId()).isEqualTo("product123");
        assertThat(request.getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should set and get userEmail")
    void setUserEmail_ShouldUpdateUserEmail() {
        AddToCartRequest request = new AddToCartRequest();
        request.setUserEmail("test@example.com");

        assertThat(request.getUserEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should set and get productId")
    void setProductId_ShouldUpdateProductId() {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId("prod456");

        assertThat(request.getProductId()).isEqualTo("prod456");
    }

    @Test
    @DisplayName("Should set and get quantity")
    void setQuantity_ShouldUpdateQuantity() {
        AddToCartRequest request = new AddToCartRequest();
        request.setQuantity(10);

        assertThat(request.getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should handle null values")
    void setNullValues_ShouldAcceptNulls() {
        AddToCartRequest request = new AddToCartRequest("user@example.com", "product123", 5);

        request.setUserEmail(null);
        request.setProductId(null);
        request.setQuantity(null);

        assertThat(request.getUserEmail()).isNull();
        assertThat(request.getProductId()).isNull();
        assertThat(request.getQuantity()).isNull();
    }

    @Test
    @DisplayName("Should create request with all valid fields")
    void createRequest_WithAllValidFields_ShouldSetCorrectly() {
        AddToCartRequest request = new AddToCartRequest();
        request.setUserEmail("customer@example.com");
        request.setProductId("laptop-001");
        request.setQuantity(3);

        assertThat(request.getUserEmail()).isEqualTo("customer@example.com");
        assertThat(request.getProductId()).isEqualTo("laptop-001");
        assertThat(request.getQuantity()).isEqualTo(3);
    }
}
