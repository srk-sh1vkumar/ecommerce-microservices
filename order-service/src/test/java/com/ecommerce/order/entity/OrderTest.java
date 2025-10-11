package com.ecommerce.order.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static com.ecommerce.order.entity.OrderStatus.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Order entity.
 * Tests entity state management and business logic.
 */
@DisplayName("Order Entity Tests")
class OrderTest {

    @Test
    @DisplayName("Constructor - Should create order with required fields")
    void constructor_ShouldCreateOrderWithRequiredFields() {
        // Act
        Order order = new Order("test@example.com", new BigDecimal("100.00"), "123 Test St");

        // Assert
        assertThat(order.getUserEmail()).isEqualTo("test@example.com");
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(order.getShippingAddress()).isEqualTo("123 Test St");
        assertThat(order.getStatus()).isEqualTo(PENDING);
        assertThat(order.getOrderDate()).isNotNull();
        assertThat(order.getOrderDate()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("SetOrderItems - Should set order items correctly")
    void setOrderItems_ShouldSetItemsCorrectly() {
        // Arrange
        Order order = new Order("test@example.com", new BigDecimal("150.00"), "123 Test St");
        List<OrderItem> items = Arrays.asList(
            new OrderItem("prod1", "Product 1", new BigDecimal("50.00"), 2),
            new OrderItem("prod2", "Product 2", new BigDecimal("50.00"), 1)
        );

        // Act
        order.setOrderItems(items);

        // Assert
        assertThat(order.getOrderItems()).hasSize(2);
        assertThat(order.getOrderItems().get(0).getProductName()).isEqualTo("Product 1");
        assertThat(order.getOrderItems().get(1).getQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("SetPaymentIntentId - Should set payment ID")
    void setPaymentIntentId_ShouldSetIdCorrectly() {
        // Arrange
        Order order = new Order("test@example.com", new BigDecimal("100.00"), "123 Test St");

        // Act
        order.setPaymentIntentId("pi_test123");

        // Assert
        assertThat(order.getPaymentIntentId()).isEqualTo("pi_test123");
    }

    @Test
    @DisplayName("SetPaymentStatus - Should set payment status")
    void setPaymentStatus_ShouldSetStatusCorrectly() {
        // Arrange
        Order order = new Order("test@example.com", new BigDecimal("100.00"), "123 Test St");

        // Act
        order.setPaymentStatus("succeeded");

        // Assert
        assertThat(order.getPaymentStatus()).isEqualTo("succeeded");
    }

    @Test
    @DisplayName("SetStatus - Should update order status")
    void setStatus_ShouldUpdateStatus() {
        // Arrange
        Order order = new Order("test@example.com", new BigDecimal("100.00"), "123 Test St");

        // Act
        order.setStatus(CONFIRMED);

        // Assert
        assertThat(order.getStatus()).isEqualTo(CONFIRMED);
    }

    @Test
    @DisplayName("GetId - Should return null for new order")
    void getId_ForNewOrder_ShouldReturnNull() {
        // Arrange
        Order order = new Order("test@example.com", new BigDecimal("100.00"), "123 Test St");

        // Assert
        assertThat(order.getId()).isNull();
    }

    @Test
    @DisplayName("SetId - Should set ID correctly")
    void setId_ShouldSetIdCorrectly() {
        // Arrange
        Order order = new Order("test@example.com", new BigDecimal("100.00"), "123 Test St");

        // Act
        order.setId("order123");

        // Assert
        assertThat(order.getId()).isEqualTo("order123");
    }
}
