package com.ecommerce.order.service;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.dto.CartItemDTO;
import com.ecommerce.order.dto.CheckoutRequest;
import com.ecommerce.order.client.CartServiceClient;
import com.ecommerce.order.client.ProductServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private CartServiceClient cartServiceClient;
    
    @Autowired
    private ProductServiceClient productServiceClient;
    
    @CacheEvict(value = {"userOrders", "ordersByStatus"}, allEntries = true)
    @Transactional
    public Order checkout(CheckoutRequest request) {
        List<CartItemDTO> cartItems = cartServiceClient.getCartItems(request.getUserEmail());

        if (cartItems.isEmpty()) {
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
        
        return orderRepository.save(order);
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