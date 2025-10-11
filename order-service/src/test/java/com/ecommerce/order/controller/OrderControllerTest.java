package com.ecommerce.order.controller;

import com.ecommerce.order.dto.CheckoutRequest;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for OrderController.
 * Tests REST endpoints for order operations.
 */
@WebMvcTest(controllers = OrderController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@org.springframework.boot.autoconfigure.EnableAutoConfiguration(
    exclude = {
        org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration.class
    }
)
@DisplayName("OrderController Tests")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private io.micrometer.core.instrument.MeterRegistry meterRegistry;

    @MockBean
    private com.ecommerce.common.metrics.MetricsService metricsService;

    @MockBean
    private com.ecommerce.common.metrics.MetricsAspect metricsAspect;

    private CheckoutRequest checkoutRequest;
    private Order testOrder;
    private List<Order> orderHistory;

    @BeforeEach
    void setUp() {
        checkoutRequest = new CheckoutRequest();
        checkoutRequest.setUserEmail("test@example.com");
        checkoutRequest.setShippingAddress("123 Test St, Test City, TC 12345");

        testOrder = new Order("test@example.com", new BigDecimal("150.00"), "123 Test St");
        testOrder.setId("order123");
        testOrder.setOrderItems(Arrays.asList(
            new OrderItem("prod1", "Product 1", new BigDecimal("50.00"), 2),
            new OrderItem("prod2", "Product 2", new BigDecimal("50.00"), 1)
        ));

        orderHistory = Arrays.asList(testOrder);
    }

    @Test
    @DisplayName("Checkout - Should return 200 with created order")
    void checkout_WithValidRequest_ShouldReturn200() throws Exception {
        // Arrange
        when(orderService.checkout(any(CheckoutRequest.class))).thenReturn(testOrder);

        // Act & Assert
        mockMvc.perform(post("/api/orders/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(checkoutRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("order123"))
            .andExpect(jsonPath("$.userEmail").value("test@example.com"))
            .andExpect(jsonPath("$.totalAmount").value(150.00))
            .andExpect(jsonPath("$.orderItems").isArray())
            .andExpect(jsonPath("$.orderItems.length()").value(2));

        verify(orderService).checkout(any(CheckoutRequest.class));
    }

    @Test
    @DisplayName("Checkout - Should return 400 when cart is empty")
    void checkout_WithEmptyCart_ShouldReturn400() throws Exception {
        // Arrange
        when(orderService.checkout(any(CheckoutRequest.class)))
            .thenThrow(new RuntimeException("Cart is empty"));

        // Act & Assert
        mockMvc.perform(post("/api/orders/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(checkoutRequest)))
            .andExpect(status().isBadRequest());

        verify(orderService).checkout(any(CheckoutRequest.class));
    }

    @Test
    @DisplayName("Get Order History - Should return 200 with orders")
    void getOrderHistory_WithValidEmail_ShouldReturn200() throws Exception {
        // Arrange
        when(orderService.getOrderHistory("test@example.com")).thenReturn(orderHistory);

        // Act & Assert
        mockMvc.perform(get("/api/orders/history/test@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value("order123"));

        verify(orderService).getOrderHistory("test@example.com");
    }

    @Test
    @DisplayName("Get Order History - Should return empty list when no orders")
    void getOrderHistory_WhenNoOrders_ShouldReturnEmptyList() throws Exception {
        // Arrange
        when(orderService.getOrderHistory("newuser@example.com"))
            .thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/orders/history/newuser@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Get Order By ID - Should return 200 with order")
    void getOrderById_WithValidId_ShouldReturn200() throws Exception {
        // Arrange
        when(orderService.getOrderById("order123")).thenReturn(testOrder);

        // Act & Assert
        mockMvc.perform(get("/api/orders/order123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("order123"))
            .andExpect(jsonPath("$.userEmail").value("test@example.com"));

        verify(orderService).getOrderById("order123");
    }

    @Test
    @DisplayName("Get Order By ID - Should return 404 when not found")
    void getOrderById_WhenNotFound_ShouldReturn404() throws Exception {
        // Arrange
        when(orderService.getOrderById("nonexistent"))
            .thenThrow(new RuntimeException("Order not found"));

        // Act & Assert
        mockMvc.perform(get("/api/orders/nonexistent"))
            .andExpect(status().isNotFound());

        verify(orderService).getOrderById("nonexistent");
    }
}
