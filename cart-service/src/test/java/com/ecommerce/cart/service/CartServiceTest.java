package com.ecommerce.cart.service;

import com.ecommerce.cart.client.ProductServiceClient;
import com.ecommerce.cart.dto.AddToCartRequest;
import com.ecommerce.cart.dto.ProductDTO;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartItemRepository;
import com.ecommerce.common.metrics.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService Tests")
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private CartService cartService;

    private AddToCartRequest addToCartRequest;
    private ProductDTO productDTO;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        addToCartRequest = new AddToCartRequest("user@example.com", "prod123", 2);

        productDTO = new ProductDTO();
        productDTO.setId("prod123");
        productDTO.setName("Laptop");
        productDTO.setPrice(new BigDecimal("999.99"));
        productDTO.setStockQuantity(100);

        cartItem = new CartItem("user@example.com", "prod123", "Laptop", new BigDecimal("999.99"), 2);
        cartItem.setId("cart1");
    }

    @Test
    @DisplayName("getCartItems should return cart items for user")
    void getCartItems_ShouldReturnItemsForUser() {
        List<CartItem> items = Arrays.asList(cartItem);
        when(cartItemRepository.findByUserEmail("user@example.com")).thenReturn(items);

        List<CartItem> result = cartService.getCartItems("user@example.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(cartItem);
        verify(cartItemRepository).findByUserEmail("user@example.com");
    }

    @Test
    @DisplayName("addToCart should add new item when item does not exist")
    void addToCart_WhenItemDoesNotExist_ShouldAddNewItem() {
        when(productServiceClient.getProductById("prod123")).thenReturn(productDTO);
        when(cartItemRepository.findByUserEmailAndProductId("user@example.com", "prod123"))
                .thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);
        doNothing().when(metricsService).incrementItemsAddedToCart();

        CartItem result = cartService.addToCart(addToCartRequest);

        assertThat(result).isNotNull();
        verify(productServiceClient).getProductById("prod123");
        verify(cartItemRepository).findByUserEmailAndProductId("user@example.com", "prod123");
        verify(cartItemRepository).save(any(CartItem.class));
        verify(metricsService).incrementItemsAddedToCart();
    }

    @Test
    @DisplayName("addToCart should update quantity when item already exists")
    void addToCart_WhenItemExists_ShouldUpdateQuantity() {
        CartItem existingItem = new CartItem("user@example.com", "prod123", "Laptop", new BigDecimal("999.99"), 3);
        existingItem.setId("cart1");

        when(productServiceClient.getProductById("prod123")).thenReturn(productDTO);
        when(cartItemRepository.findByUserEmailAndProductId("user@example.com", "prod123"))
                .thenReturn(Optional.of(existingItem));
        when(cartItemRepository.save(existingItem)).thenReturn(existingItem);
        doNothing().when(metricsService).incrementItemsAddedToCart();

        CartItem result = cartService.addToCart(addToCartRequest);

        assertThat(result.getQuantity()).isEqualTo(5);
        verify(cartItemRepository).save(existingItem);
        verify(metricsService).incrementItemsAddedToCart();
    }

    @Test
    @DisplayName("addToCart should throw exception when product not found")
    void addToCart_WhenProductNotFound_ShouldThrowException() {
        when(productServiceClient.getProductById("prod123")).thenReturn(null);

        assertThatThrownBy(() -> cartService.addToCart(addToCartRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Product not found");
    }

    @Test
    @DisplayName("addToCart should throw exception when insufficient stock")
    void addToCart_WhenInsufficientStock_ShouldThrowException() {
        productDTO.setStockQuantity(1);
        when(productServiceClient.getProductById("prod123")).thenReturn(productDTO);

        assertThatThrownBy(() -> cartService.addToCart(addToCartRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Insufficient stock");
    }

    @Test
    @DisplayName("updateCartItemQuantity should update quantity")
    void updateCartItemQuantity_ShouldUpdateQuantity() {
        when(cartItemRepository.findByUserEmailAndProductId("user@example.com", "prod123"))
                .thenReturn(Optional.of(cartItem));
        when(cartItemRepository.save(cartItem)).thenReturn(cartItem);

        CartItem result = cartService.updateCartItemQuantity("user@example.com", "prod123", 5);

        assertThat(result.getQuantity()).isEqualTo(5);
        verify(cartItemRepository).save(cartItem);
    }

    @Test
    @DisplayName("updateCartItemQuantity should delete item when quantity is zero")
    void updateCartItemQuantity_WhenQuantityZero_ShouldDeleteItem() {
        when(cartItemRepository.findByUserEmailAndProductId("user@example.com", "prod123"))
                .thenReturn(Optional.of(cartItem));
        doNothing().when(cartItemRepository).delete(cartItem);

        CartItem result = cartService.updateCartItemQuantity("user@example.com", "prod123", 0);

        assertThat(result).isNull();
        verify(cartItemRepository).delete(cartItem);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateCartItemQuantity should delete item when quantity is negative")
    void updateCartItemQuantity_WhenQuantityNegative_ShouldDeleteItem() {
        when(cartItemRepository.findByUserEmailAndProductId("user@example.com", "prod123"))
                .thenReturn(Optional.of(cartItem));
        doNothing().when(cartItemRepository).delete(cartItem);

        CartItem result = cartService.updateCartItemQuantity("user@example.com", "prod123", -1);

        assertThat(result).isNull();
        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    @DisplayName("updateCartItemQuantity should throw exception when item not found")
    void updateCartItemQuantity_WhenItemNotFound_ShouldThrowException() {
        when(cartItemRepository.findByUserEmailAndProductId("user@example.com", "prod123"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateCartItemQuantity("user@example.com", "prod123", 5))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cart item not found");
    }

    @Test
    @DisplayName("removeFromCart should remove item from cart")
    void removeFromCart_ShouldRemoveItem() {
        doNothing().when(cartItemRepository).deleteByUserEmailAndProductId("user@example.com", "prod123");
        doNothing().when(metricsService).incrementItemsRemovedFromCart();

        cartService.removeFromCart("user@example.com", "prod123");

        verify(cartItemRepository).deleteByUserEmailAndProductId("user@example.com", "prod123");
        verify(metricsService).incrementItemsRemovedFromCart();
    }

    @Test
    @DisplayName("clearCart should remove all items for user")
    void clearCart_ShouldRemoveAllItems() {
        doNothing().when(cartItemRepository).deleteByUserEmail("user@example.com");
        doNothing().when(metricsService).incrementCartsCleared();

        cartService.clearCart("user@example.com");

        verify(cartItemRepository).deleteByUserEmail("user@example.com");
        verify(metricsService).incrementCartsCleared();
    }

    @Test
    @DisplayName("getCartTotal should calculate total price")
    void getCartTotal_ShouldCalculateTotalPrice() {
        CartItem item1 = new CartItem("user@example.com", "prod1", "Item1", new BigDecimal("10.00"), 2);
        CartItem item2 = new CartItem("user@example.com", "prod2", "Item2", new BigDecimal("20.00"), 3);
        List<CartItem> items = Arrays.asList(item1, item2);

        when(cartItemRepository.findByUserEmail("user@example.com")).thenReturn(items);

        BigDecimal total = cartService.getCartTotal("user@example.com");

        assertThat(total).isEqualByComparingTo(new BigDecimal("80.00"));
        verify(cartItemRepository).findByUserEmail("user@example.com");
    }

    @Test
    @DisplayName("getCartTotal should return zero for empty cart")
    void getCartTotal_WhenCartEmpty_ShouldReturnZero() {
        when(cartItemRepository.findByUserEmail("user@example.com")).thenReturn(Collections.emptyList());

        BigDecimal total = cartService.getCartTotal("user@example.com");

        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
