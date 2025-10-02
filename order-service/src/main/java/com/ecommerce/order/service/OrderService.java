package com.ecommerce.order.service;

import com.ecommerce.common.metrics.MetricsService;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.dto.CartItemDTO;
import com.ecommerce.order.dto.CheckoutRequest;
import com.ecommerce.order.client.CartServiceClient;
import com.ecommerce.order.client.ProductServiceClient;
import com.ecommerce.order.client.NotificationServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartServiceClient cartServiceClient;

    @Autowired
    private ProductServiceClient productServiceClient;

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private NotificationServiceClient notificationServiceClient;
    
    @CacheEvict(value = {"userOrders", "ordersByStatus"}, allEntries = true)
    @Transactional
    public Order checkout(CheckoutRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            List<CartItemDTO> cartItems = cartServiceClient.getCartItems(request.getUserEmail());

            if (cartItems.isEmpty()) {
                metricsService.incrementOrdersFailed();
                throw new RuntimeException("Cart is empty");
            }

        // Bulk update stock - reduces N+1 query problem
        List<com.ecommerce.order.dto.StockUpdateRequest> stockUpdates = cartItems.stream()
                .map(item -> new com.ecommerce.order.dto.StockUpdateRequest(item.getProductId(), item.getQuantity()))
                .collect(java.util.stream.Collectors.toList());

        com.ecommerce.order.dto.BulkStockUpdateResponse response = productServiceClient.bulkUpdateStock(stockUpdates);

        if (!response.allSuccessful()) {
            // Find which products failed
            List<String> failedProducts = response.getResults().entrySet().stream()
                    .filter(entry -> !entry.getValue())
                    .map(entry -> {
                        CartItemDTO item = cartItems.stream()
                                .filter(ci -> ci.getProductId().equals(entry.getKey()))
                                .findFirst()
                                .orElse(null);
                        return item != null ? item.getProductName() : entry.getKey();
                    })
                    .collect(java.util.stream.Collectors.toList());

            metricsService.incrementOrdersFailed();
            throw new RuntimeException("Insufficient stock for products: " + String.join(", ", failedProducts));
        }

        BigDecimal totalAmount = cartItems.stream()
                .map(item -> item.getProductPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Order order = new Order(request.getUserEmail(), totalAmount, request.getShippingAddress());
        order = orderRepository.save(order);
        
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItemDTO cartItem : cartItems) {
            OrderItem orderItem = new OrderItem(
                    cartItem.getProductId(),
                    cartItem.getProductName(),
                    cartItem.getProductPrice(),
                    cartItem.getQuantity()
            );
            orderItems.add(orderItem);
        }
        order.setOrderItems(orderItems);


        cartServiceClient.clearCart(request.getUserEmail());

        Order savedOrder = orderRepository.save(order);

        // Record successful checkout
        metricsService.incrementOrdersPlaced();
        long duration = System.currentTimeMillis() - startTime;
        metricsService.recordCheckoutDuration(duration, TimeUnit.MILLISECONDS);

        // Send order confirmation email asynchronously
        sendOrderConfirmationEmailAsync(savedOrder, request.getUserEmail());

        return savedOrder;
        } catch (Exception e) {
            metricsService.incrementOrdersFailed();
            throw e;
        }
    }

    /**
     * Sends order confirmation email asynchronously.
     * Email sending failures do not affect order processing.
     *
     * @param order The created order
     * @param userEmail Customer's email address
     */
    @Async
    protected void sendOrderConfirmationEmailAsync(Order order, String userEmail) {
        try {
            // Extract user's first name from email (or could call user-service)
            String userName = userEmail.substring(0, userEmail.indexOf('@'));

            // Format order items for email
            StringBuilder orderItemsText = new StringBuilder();
            for (OrderItem item : order.getOrderItems()) {
                orderItemsText.append(String.format("%s x%d - $%s\n",
                    item.getProductName(),
                    item.getQuantity(),
                    item.getProductPrice().multiply(BigDecimal.valueOf(item.getQuantity()))));
            }

            notificationServiceClient.sendOrderConfirmation(
                userEmail,
                userName,
                order.getId(),
                "$" + order.getTotalAmount().toString(),
                orderItemsText.toString()
            );

            logger.info("Order confirmation email sent successfully for order: {}", order.getId());
        } catch (Exception e) {
            // Log error but don't fail order processing
            logger.error("Failed to send order confirmation email for order: {}. Error: {}",
                        order.getId(), e.getMessage());
        }
    }
    
    @Cacheable(value = "userOrders", key = "#userEmail")
    public List<Order> getOrderHistory(String userEmail) {
        return orderRepository.findByUserEmailOrderByOrderDateDesc(userEmail);
    }
    
    @Cacheable(value = "order", key = "#orderId")
    public Order getOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }
}