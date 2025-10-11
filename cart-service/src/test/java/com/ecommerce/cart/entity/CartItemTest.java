package com.ecommerce.cart.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CartItem entity.
 */
@DisplayName("CartItem Entity Tests")
class CartItemTest {

    @Test
    @DisplayName("Default constructor should create empty cart item")
    void defaultConstructor_ShouldCreateEmptyCartItem() {
        CartItem cartItem = new CartItem();

        assertThat(cartItem.getId()).isNull();
        assertThat(cartItem.getUserEmail()).isNull();
        assertThat(cartItem.getProductId()).isNull();
        assertThat(cartItem.getProductName()).isNull();
        assertThat(cartItem.getProductPrice()).isNull();
        assertThat(cartItem.getQuantity()).isNull();
        assertThat(cartItem.getAddedAt()).isNotNull(); // Initialized to now()
    }

    @Test
    @DisplayName("Parameterized constructor should set all fields")
    void parameterizedConstructor_ShouldSetAllFields() {
        CartItem cartItem = new CartItem(
                "user@example.com",
                "product123",
                "Laptop",
                new BigDecimal("999.99"),
                2
        );

        assertThat(cartItem.getUserEmail()).isEqualTo("user@example.com");
        assertThat(cartItem.getProductId()).isEqualTo("product123");
        assertThat(cartItem.getProductName()).isEqualTo("Laptop");
        assertThat(cartItem.getProductPrice()).isEqualTo(new BigDecimal("999.99"));
        assertThat(cartItem.getQuantity()).isEqualTo(2);
        assertThat(cartItem.getAddedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should set and get id")
    void setId_ShouldUpdateId() {
        CartItem cartItem = new CartItem();
        cartItem.setId("cart123");

        assertThat(cartItem.getId()).isEqualTo("cart123");
    }

    @Test
    @DisplayName("Should set and get userEmail")
    void setUserEmail_ShouldUpdateUserEmail() {
        CartItem cartItem = new CartItem();
        cartItem.setUserEmail("test@example.com");

        assertThat(cartItem.getUserEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should set and get productId")
    void setProductId_ShouldUpdateProductId() {
        CartItem cartItem = new CartItem();
        cartItem.setProductId("prod456");

        assertThat(cartItem.getProductId()).isEqualTo("prod456");
    }

    @Test
    @DisplayName("Should set and get productName")
    void setProductName_ShouldUpdateProductName() {
        CartItem cartItem = new CartItem();
        cartItem.setProductName("Smartphone");

        assertThat(cartItem.getProductName()).isEqualTo("Smartphone");
    }

    @Test
    @DisplayName("Should set and get productPrice")
    void setProductPrice_ShouldUpdateProductPrice() {
        CartItem cartItem = new CartItem();
        cartItem.setProductPrice(new BigDecimal("599.99"));

        assertThat(cartItem.getProductPrice()).isEqualTo(new BigDecimal("599.99"));
    }

    @Test
    @DisplayName("Should set and get quantity")
    void setQuantity_ShouldUpdateQuantity() {
        CartItem cartItem = new CartItem();
        cartItem.setQuantity(5);

        assertThat(cartItem.getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should set and get addedAt")
    void setAddedAt_ShouldUpdateAddedAt() {
        CartItem cartItem = new CartItem();
        LocalDateTime now = LocalDateTime.now();
        cartItem.setAddedAt(now);

        assertThat(cartItem.getAddedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("getTotalPrice should calculate correctly")
    void getTotalPrice_ShouldCalculateCorrectly() {
        CartItem cartItem = new CartItem();
        cartItem.setProductPrice(new BigDecimal("50.00"));
        cartItem.setQuantity(3);

        BigDecimal totalPrice = cartItem.getTotalPrice();

        assertThat(totalPrice).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    @DisplayName("getTotalPrice with decimal price should calculate correctly")
    void getTotalPrice_WithDecimalPrice_ShouldCalculateCorrectly() {
        CartItem cartItem = new CartItem();
        cartItem.setProductPrice(new BigDecimal("19.99"));
        cartItem.setQuantity(4);

        BigDecimal totalPrice = cartItem.getTotalPrice();

        assertThat(totalPrice).isEqualByComparingTo(new BigDecimal("79.96"));
    }

    @Test
    @DisplayName("getTotalPrice with quantity 1 should return product price")
    void getTotalPrice_WithQuantityOne_ShouldReturnProductPrice() {
        CartItem cartItem = new CartItem();
        cartItem.setProductPrice(new BigDecimal("100.00"));
        cartItem.setQuantity(1);

        BigDecimal totalPrice = cartItem.getTotalPrice();

        assertThat(totalPrice).isEqualByComparingTo(new BigDecimal("100.00"));
    }
}
