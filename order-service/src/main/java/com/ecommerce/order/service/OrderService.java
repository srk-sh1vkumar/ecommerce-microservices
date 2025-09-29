package com.ecommerce.order.service;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.dto.CartItemDTO;
import com.ecommerce.order.dto.CheckoutRequest;
import com.ecommerce.order.client.CartServiceClient;
import com.ecommerce.order.client.ProductServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    @Transactional
    public Order checkout(CheckoutRequest request) {
        List<CartItemDTO> cartItems = cartServiceClient.getCartItems(request.getUserEmail());
        
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }
        
        for (CartItemDTO item : cartItems) {
            Boolean stockUpdated = productServiceClient.updateStock(item.getProductId(), item.getQuantity());
            if (!stockUpdated) {
                throw new RuntimeException("Insufficient stock for product: " + item.getProductName());
            }
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
    
    public List<Order> getOrderHistory(String userEmail) {
        return orderRepository.findByUserEmailOrderByOrderDateDesc(userEmail);
    }
    
    public Order getOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }
}