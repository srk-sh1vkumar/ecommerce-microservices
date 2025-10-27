package com.ecommerce.order.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for StockUpdateRequest DTO.
 */
@DisplayName("StockUpdateRequest Tests")
class StockUpdateRequestTest {

    @Test
    @DisplayName("Default constructor should create empty object")
    void defaultConstructor_ShouldCreateEmptyObject() {
        StockUpdateRequest request = new StockUpdateRequest();

        assertThat(request.getProductId()).isNull();
        assertThat(request.getQuantity()).isNull();
    }

    @Test
    @DisplayName("Parameterized constructor should set all fields")
    void parameterizedConstructor_ShouldSetAllFields() {
        StockUpdateRequest request = new StockUpdateRequest("prod123", 10);

        assertThat(request.getProductId()).isEqualTo("prod123");
        assertThat(request.getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("Setters should update fields correctly")
    void setters_ShouldUpdateFields() {
        StockUpdateRequest request = new StockUpdateRequest();

        request.setProductId("prod456");
        request.setQuantity(25);

        assertThat(request.getProductId()).isEqualTo("prod456");
        assertThat(request.getQuantity()).isEqualTo(25);
    }
}
