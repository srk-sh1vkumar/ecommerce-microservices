package com.ecommerce.product.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for StockUpdateRequest DTO.
 * Tests request construction and field access.
 */
@DisplayName("StockUpdateRequest Tests")
class StockUpdateRequestTest {

    @Test
    @DisplayName("Default constructor should create empty request")
    void defaultConstructor_ShouldCreateEmptyRequest() {
        // Act
        StockUpdateRequest request = new StockUpdateRequest();

        // Assert
        assertThat(request).isNotNull();
        assertThat(request.getProductId()).isNull();
        assertThat(request.getQuantity()).isNull();
    }

    @Test
    @DisplayName("Parameterized constructor should set all fields")
    void parameterizedConstructor_ShouldSetAllFields() {
        // Arrange
        String productId = "prod123";
        Integer quantity = 50;

        // Act
        StockUpdateRequest request = new StockUpdateRequest(productId, quantity);

        // Assert
        assertThat(request.getProductId()).isEqualTo(productId);
        assertThat(request.getQuantity()).isEqualTo(quantity);
    }

    @Test
    @DisplayName("setProductId should update product ID")
    void setProductId_ShouldUpdateProductId() {
        // Arrange
        StockUpdateRequest request = new StockUpdateRequest();
        String productId = "prod456";

        // Act
        request.setProductId(productId);

        // Assert
        assertThat(request.getProductId()).isEqualTo(productId);
    }

    @Test
    @DisplayName("setQuantity should update quantity")
    void setQuantity_ShouldUpdateQuantity() {
        // Arrange
        StockUpdateRequest request = new StockUpdateRequest();
        Integer quantity = 100;

        // Act
        request.setQuantity(quantity);

        // Assert
        assertThat(request.getQuantity()).isEqualTo(quantity);
    }

    @Test
    @DisplayName("Should handle null values")
    void setters_WithNullValues_ShouldAccept() {
        // Arrange
        StockUpdateRequest request = new StockUpdateRequest("prod1", 10);

        // Act
        request.setProductId(null);
        request.setQuantity(null);

        // Assert
        assertThat(request.getProductId()).isNull();
        assertThat(request.getQuantity()).isNull();
    }

    @Test
    @DisplayName("Should create request with all fields populated")
    void fullRequest_ShouldHaveAllFieldsSet() {
        // Arrange & Act
        StockUpdateRequest request = new StockUpdateRequest();
        request.setProductId("prod789");
        request.setQuantity(25);

        // Assert
        assertThat(request.getProductId()).isEqualTo("prod789");
        assertThat(request.getQuantity()).isEqualTo(25);
    }
}
