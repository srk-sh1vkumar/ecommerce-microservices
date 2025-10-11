package com.ecommerce.cart.controller;

import com.ecommerce.cart.dto.AddToCartRequest;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartController Tests")
class CartControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    private CartItem cartItem;
    private AddToCartRequest addToCartRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cartController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        cartItem = new CartItem("user@example.com", "prod123", "Laptop", new BigDecimal("999.99"), 2);
        cartItem.setId("cart1");

        addToCartRequest = new AddToCartRequest("user@example.com", "prod123", 2);
    }

    @Test
    @DisplayName("GET /api/cart/{userEmail} - Should return cart items")
    void getCartItems_ShouldReturnCartItems() throws Exception {
        List<CartItem> items = Arrays.asList(cartItem);
        when(cartService.getCartItems("user@example.com")).thenReturn(items);

        mockMvc.perform(get("/api/cart/user@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userEmail").value("user@example.com"))
                .andExpect(jsonPath("$[0].productId").value("prod123"))
                .andExpect(jsonPath("$[0].productName").value("Laptop"));

        verify(cartService).getCartItems("user@example.com");
    }

    @Test
    @DisplayName("GET /api/cart/{userEmail} - Should return empty list for empty cart")
    void getCartItems_WhenCartEmpty_ShouldReturnEmptyList() throws Exception {
        when(cartService.getCartItems("user@example.com")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/cart/user@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("POST /api/cart/add - Should add item to cart")
    void addToCart_ShouldAddItem() throws Exception {
        when(cartService.addToCart(any(AddToCartRequest.class))).thenReturn(cartItem);

        mockMvc.perform(post("/api/cart/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToCartRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userEmail").value("user@example.com"))
                .andExpect(jsonPath("$.productId").value("prod123"));

        verify(cartService).addToCart(any(AddToCartRequest.class));
    }

    @Test
    @DisplayName("POST /api/cart/add - Should return bad request when exception occurs")
    void addToCart_WhenExceptionOccurs_ShouldReturnBadRequest() throws Exception {
        when(cartService.addToCart(any(AddToCartRequest.class)))
                .thenThrow(new RuntimeException("Product not found"));

        mockMvc.perform(post("/api/cart/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToCartRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/cart/{userEmail}/{productId} - Should update cart item quantity")
    void updateCartItemQuantity_ShouldUpdateQuantity() throws Exception {
        cartItem.setQuantity(5);
        when(cartService.updateCartItemQuantity("user@example.com", "prod123", 5)).thenReturn(cartItem);

        mockMvc.perform(put("/api/cart/user@example.com/prod123")
                        .param("quantity", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(5));

        verify(cartService).updateCartItemQuantity("user@example.com", "prod123", 5);
    }

    @Test
    @DisplayName("PUT /api/cart/{userEmail}/{productId} - Should return bad request when exception occurs")
    void updateCartItemQuantity_WhenExceptionOccurs_ShouldReturnBadRequest() throws Exception {
        when(cartService.updateCartItemQuantity("user@example.com", "prod123", 5))
                .thenThrow(new RuntimeException("Cart item not found"));

        mockMvc.perform(put("/api/cart/user@example.com/prod123")
                        .param("quantity", "5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/cart/{userEmail}/{productId} - Should remove item from cart")
    void removeFromCart_ShouldRemoveItem() throws Exception {
        doNothing().when(cartService).removeFromCart("user@example.com", "prod123");

        mockMvc.perform(delete("/api/cart/user@example.com/prod123"))
                .andExpect(status().isOk());

        verify(cartService).removeFromCart("user@example.com", "prod123");
    }

    @Test
    @DisplayName("DELETE /api/cart/{userEmail} - Should clear cart")
    void clearCart_ShouldClearCart() throws Exception {
        doNothing().when(cartService).clearCart("user@example.com");

        mockMvc.perform(delete("/api/cart/user@example.com"))
                .andExpect(status().isOk());

        verify(cartService).clearCart("user@example.com");
    }

    @Test
    @DisplayName("GET /api/cart/{userEmail}/total - Should return cart total")
    void getCartTotal_ShouldReturnTotal() throws Exception {
        when(cartService.getCartTotal("user@example.com")).thenReturn(new BigDecimal("1999.98"));

        mockMvc.perform(get("/api/cart/user@example.com/total"))
                .andExpect(status().isOk())
                .andExpect(content().string("1999.98"));

        verify(cartService).getCartTotal("user@example.com");
    }

    @Test
    @DisplayName("GET /api/cart/{userEmail}/total - Should return zero for empty cart")
    void getCartTotal_WhenCartEmpty_ShouldReturnZero() throws Exception {
        when(cartService.getCartTotal("user@example.com")).thenReturn(BigDecimal.ZERO);

        mockMvc.perform(get("/api/cart/user@example.com/total"))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }
}
