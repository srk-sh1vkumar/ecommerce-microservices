package com.ecommerce.order.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OrderItem entity.
 */
@DisplayName("OrderItem Tests")
class OrderItemTest {

    @Test
    @DisplayName("Default constructor should create empty object")
    void defaultConstructor_ShouldCreateEmptyObject() {
        OrderItem item = new OrderItem();

        assertThat(item.getProductId()).isNull();
        assertThat(item.getProductName()).isNull();
        assertThat(item.getProductPrice()).isNull();
        assertThat(item.getQuantity()).isNull();
    }

    @Test
    @DisplayName("Parameterized constructor should set all fields")
    void parameterizedConstructor_ShouldSetAllFields() {
        OrderItem item = new OrderItem("prod123", "Test Product", new BigDecimal("99.99"), 2);

        assertThat(item.getProductId()).isEqualTo("prod123");
        assertThat(item.getProductName()).isEqualTo("Test Product");
        assertThat(item.getProductPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(item.getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("Setters should update all fields correctly")
    void setters_ShouldUpdateAllFields() {
        OrderItem item = new OrderItem();

        item.setProductId("prod456");
        item.setProductName("Updated Product");
        item.setProductPrice(new BigDecimal("149.99"));
        item.setQuantity(3);

        assertThat(item.getProductId()).isEqualTo("prod456");
        assertThat(item.getProductName()).isEqualTo("Updated Product");
        assertThat(item.getProductPrice()).isEqualByComparingTo(new BigDecimal("149.99"));
        assertThat(item.getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("getTotalPrice should calculate correctly")
    void getTotalPrice_ShouldCalculateCorrectly() {
        OrderItem item = new OrderItem("prod123", "Test Product", new BigDecimal("50.00"), 3);

        BigDecimal totalPrice = item.getTotalPrice();

        assertThat(totalPrice).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    @DisplayName("getTotalPrice should handle decimal quantities correctly")
    void getTotalPrice_WithDecimals_ShouldCalculateCorrectly() {
        OrderItem item = new OrderItem("prod456", "Product", new BigDecimal("29.99"), 5);

        BigDecimal totalPrice = item.getTotalPrice();

        assertThat(totalPrice).isEqualByComparingTo(new BigDecimal("149.95"));
    }
}
