package com.ecommerce.order.service;

import com.ecommerce.common.metrics.MetricsService;
import com.ecommerce.order.client.CartServiceClient;
import com.ecommerce.order.client.NotificationServiceClient;
import com.ecommerce.order.client.ProductServiceClient;
import com.ecommerce.order.dto.BulkStockUpdateResponse;
import com.ecommerce.order.dto.CartItemDTO;
import com.ecommerce.order.dto.CheckoutRequest;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for OrderService.
 * Tests checkout process, order management, and stock validation.
 *
 * @author E-commerce Development Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartServiceClient cartServiceClient;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private MetricsService metricsService;

    @Mock
    private NotificationServiceClient notificationServiceClient;

    @InjectMocks
    private OrderService orderService;

    private CheckoutRequest checkoutRequest;
    private List<CartItemDTO> cartItems;
    private Order savedOrder;

    @BeforeEach
    void setUp() {
        // Setup checkout request
        checkoutRequest = new CheckoutRequest();
        checkoutRequest.setUserEmail("test@example.com");
        checkoutRequest.setShippingAddress("123 Test St, Test City, TC 12345");

        // Setup cart items
        CartItemDTO item1 = new CartItemDTO();
        item1.setProductId("prod1");
        item1.setProductName("Product 1");
        item1.setProductPrice(new BigDecimal("50.00"));
        item1.setQuantity(2);

        CartItemDTO item2 = new CartItemDTO();
        item2.setProductId("prod2");
        item2.setProductName("Product 2");
        item2.setProductPrice(new BigDecimal("30.00"));
        item2.setQuantity(1);

        cartItems = Arrays.asList(item1, item2);

        // Setup saved order
        savedOrder = new Order("test@example.com", new BigDecimal("130.00"), "123 Test St");
        savedOrder.setId("order123");
        savedOrder.setOrderItems(Arrays.asList(
            new OrderItem("prod1", "Product 1", new BigDecimal("50.00"), 2),
            new OrderItem("prod2", "Product 2", new BigDecimal("30.00"), 1)
        ));
    }

    // ==================== Checkout Tests ====================

    @Test
    @DisplayName("Checkout - Should create order successfully with valid cart")
    void checkout_WithValidCart_ShouldCreateOrder() {
        // Arrange
        when(cartServiceClient.getCartItems("test@example.com")).thenReturn(cartItems);

        Map<String, Boolean> stockResults = new HashMap<>();
        stockResults.put("prod1", true);
        stockResults.put("prod2", true);
        BulkStockUpdateResponse stockResponse = new BulkStockUpdateResponse(stockResults);
        when(productServiceClient.bulkUpdateStock(anyList())).thenReturn(stockResponse);

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        doNothing().when(cartServiceClient).clearCart(anyString());

        // Act
        Order result = orderService.checkout(checkoutRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("order123");
        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("130.00"));
        assertThat(result.getOrderItems()).hasSize(2);

        verify(cartServiceClient).getCartItems("test@example.com");
        verify(productServiceClient).bulkUpdateStock(anyList());
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(cartServiceClient).clearCart("test@example.com");
        verify(metricsService).incrementOrdersPlaced();
        verify(metricsService).recordCheckoutDuration(anyLong(), any());
    }

    @Test
    @DisplayName("Checkout - Should throw exception when cart is empty")
    void checkout_WithEmptyCart_ShouldThrowException() {
        // Arrange
        when(cartServiceClient.getCartItems("test@example.com")).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThatThrownBy(() -> orderService.checkout(checkoutRequest))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Cart is empty");

        verify(cartServiceClient).getCartItems("test@example.com");
        verify(productServiceClient, never()).bulkUpdateStock(anyList());
        verify(orderRepository, never()).save(any(Order.class));
        verify(metricsService).incrementOrdersFailed();
    }

    @Test
    @DisplayName("Checkout - Should fail when product stock is insufficient")
    void checkout_WithInsufficientStock_ShouldThrowException() {
        // Arrange
        when(cartServiceClient.getCartItems("test@example.com")).thenReturn(cartItems);

        Map<String, Boolean> stockResults = new HashMap<>();
        stockResults.put("prod1", true);
        stockResults.put("prod2", false);  // Failed stock update
        BulkStockUpdateResponse stockResponse = new BulkStockUpdateResponse(stockResults);
        when(productServiceClient.bulkUpdateStock(anyList())).thenReturn(stockResponse);

        // Act & Assert
        assertThatThrownBy(() -> orderService.checkout(checkoutRequest))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Insufficient stock");

        verify(cartServiceClient).getCartItems("test@example.com");
        verify(productServiceClient).bulkUpdateStock(anyList());
        verify(orderRepository, never()).save(any(Order.class));
        verify(metricsService).incrementOrdersFailed();
    }

    @Test
    @DisplayName("Checkout - Should calculate total amount correctly")
    void checkout_ShouldCalculateCorrectTotal() {
        // Arrange
        when(cartServiceClient.getCartItems("test@example.com")).thenReturn(cartItems);

        Map<String, Boolean> stockResults = new HashMap<>();
        stockResults.put("prod1", true);
        stockResults.put("prod2", true);
        BulkStockUpdateResponse stockResponse = new BulkStockUpdateResponse(stockResults);
        when(productServiceClient.bulkUpdateStock(anyList())).thenReturn(stockResponse);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(cartServiceClient).clearCart(anyString());

        // Act
        Order result = orderService.checkout(checkoutRequest);

        // Assert
        // Total = (50 * 2) + (30 * 1) = 130
        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("130.00"));
        verify(orderRepository, atLeastOnce()).save(argThat(order ->
            order.getTotalAmount().compareTo(new BigDecimal("130.00")) == 0
        ));
    }

    // ==================== Get Order History Tests ====================

    @Test
    @DisplayName("GetOrderHistory - Should return orders for user")
    void getOrderHistory_ShouldReturnUserOrders() {
        // Arrange
        List<Order> orders = Arrays.asList(savedOrder, savedOrder);
        when(orderRepository.findByUserEmailOrderByOrderDateDesc("test@example.com"))
            .thenReturn(orders);

        // Act
        List<Order> result = orderService.getOrderHistory("test@example.com");

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(order -> order.getUserEmail().equals("test@example.com"));
        verify(orderRepository).findByUserEmailOrderByOrderDateDesc("test@example.com");
    }

    @Test
    @DisplayName("GetOrderHistory - Should return empty list when no orders")
    void getOrderHistory_WhenNoOrders_ShouldReturnEmptyList() {
        // Arrange
        when(orderRepository.findByUserEmailOrderByOrderDateDesc("newuser@example.com"))
            .thenReturn(Collections.emptyList());

        // Act
        List<Order> result = orderService.getOrderHistory("newuser@example.com");

        // Assert
        assertThat(result).isEmpty();
        verify(orderRepository).findByUserEmailOrderByOrderDateDesc("newuser@example.com");
    }

    // ==================== Get Order By ID Tests ====================

    @Test
    @DisplayName("GetOrderById - Should return order when exists")
    void getOrderById_WhenExists_ShouldReturnOrder() {
        // Arrange
        when(orderRepository.findById("order123")).thenReturn(Optional.of(savedOrder));

        // Act
        Order result = orderService.getOrderById("order123");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("order123");
        verify(orderRepository).findById("order123");
    }

    @Test
    @DisplayName("GetOrderById - Should throw exception when not found")
    void getOrderById_WhenNotFound_ShouldThrowException() {
        // Arrange
        when(orderRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.getOrderById("nonexistent"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Order not found");

        verify(orderRepository).findById("nonexistent");
    }

    // ==================== Order Items Tests ====================

    @Test
    @DisplayName("Checkout - Should create correct order items from cart")
    void checkout_ShouldCreateOrderItemsFromCart() {
        // Arrange
        when(cartServiceClient.getCartItems("test@example.com")).thenReturn(cartItems);

        Map<String, Boolean> stockResults = new HashMap<>();
        stockResults.put("prod1", true);
        stockResults.put("prod2", true);
        BulkStockUpdateResponse stockResponse = new BulkStockUpdateResponse(stockResults);
        when(productServiceClient.bulkUpdateStock(anyList())).thenReturn(stockResponse);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(cartServiceClient).clearCart(anyString());

        // Act
        Order result = orderService.checkout(checkoutRequest);

        // Assert
        assertThat(result.getOrderItems()).hasSize(2);
        assertThat(result.getOrderItems().get(0).getProductId()).isEqualTo("prod1");
        assertThat(result.getOrderItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(result.getOrderItems().get(1).getProductId()).isEqualTo("prod2");
        assertThat(result.getOrderItems().get(1).getQuantity()).isEqualTo(1);
    }

    // ==================== Metrics Tests ====================

    @Test
    @DisplayName("Checkout - Should record metrics on successful checkout")
    void checkout_OnSuccess_ShouldRecordMetrics() {
        // Arrange
        when(cartServiceClient.getCartItems("test@example.com")).thenReturn(cartItems);

        Map<String, Boolean> stockResults = new HashMap<>();
        stockResults.put("prod1", true);
        stockResults.put("prod2", true);
        BulkStockUpdateResponse stockResponse = new BulkStockUpdateResponse(stockResults);
        when(productServiceClient.bulkUpdateStock(anyList())).thenReturn(stockResponse);

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        doNothing().when(cartServiceClient).clearCart(anyString());

        // Act
        orderService.checkout(checkoutRequest);

        // Assert
        verify(metricsService).incrementOrdersPlaced();
        verify(metricsService).recordCheckoutDuration(anyLong(), any());
        verify(metricsService, never()).incrementOrdersFailed();
    }

    @Test
    @DisplayName("Checkout - Should record failure metrics on error")
    void checkout_OnFailure_ShouldRecordFailureMetrics() {
        // Arrange
        when(cartServiceClient.getCartItems("test@example.com")).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThatThrownBy(() -> orderService.checkout(checkoutRequest))
            .isInstanceOf(RuntimeException.class);

        verify(metricsService).incrementOrdersFailed();
        verify(metricsService, never()).incrementOrdersPlaced();
    }
}
