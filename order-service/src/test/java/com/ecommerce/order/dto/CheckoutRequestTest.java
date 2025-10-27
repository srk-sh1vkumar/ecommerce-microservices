package com.ecommerce.order.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CheckoutRequest DTO.
 */
@DisplayName("CheckoutRequest Tests")
class CheckoutRequestTest {

    @Test
    @DisplayName("Default constructor should create empty object")
    void defaultConstructor_ShouldCreateEmptyObject() {
        CheckoutRequest request = new CheckoutRequest();

        assertThat(request.getUserEmail()).isNull();
        assertThat(request.getShippingAddress()).isNull();
    }

    @Test
    @DisplayName("Parameterized constructor should set all fields")
    void parameterizedConstructor_ShouldSetAllFields() {
        CheckoutRequest request = new CheckoutRequest("user@example.com", "123 Main St, City, State 12345");

        assertThat(request.getUserEmail()).isEqualTo("user@example.com");
        assertThat(request.getShippingAddress()).isEqualTo("123 Main St, City, State 12345");
    }

    @Test
    @DisplayName("Setters should update fields correctly")
    void setters_ShouldUpdateFields() {
        CheckoutRequest request = new CheckoutRequest();

        request.setUserEmail("test@example.com");
        request.setShippingAddress("456 Oak Ave, Town, State 67890");

        assertThat(request.getUserEmail()).isEqualTo("test@example.com");
        assertThat(request.getShippingAddress()).isEqualTo("456 Oak Ave, Town, State 67890");
    }
}
