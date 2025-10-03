package com.ecommerce.order.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "orders")
@CompoundIndexes({
    @CompoundIndex(name = "user_date_idx", def = "{'userEmail': 1, 'orderDate': -1}"),
    @CompoundIndex(name = "status_date_idx", def = "{'status': 1, 'orderDate': -1}"),
    @CompoundIndex(name = "user_status_idx", def = "{'userEmail': 1, 'status': 1}")
})
public class Order {
    @Id
    private String id;

    @Indexed
    private String userEmail;

    private BigDecimal totalAmount;
    @Indexed
    private OrderStatus status = OrderStatus.PENDING;
    private String shippingAddress;
    @Indexed
    private LocalDateTime orderDate = LocalDateTime.now();
    private List<OrderItem> orderItems;

    // Payment information
    @Indexed
    private String paymentIntentId;
    private String paymentStatus;
    private LocalDateTime paymentDate;
    
    public Order() {}
    
    public Order(String userEmail, BigDecimal totalAmount, String shippingAddress) {
        this.userEmail = userEmail;
        this.totalAmount = totalAmount;
        this.shippingAddress = shippingAddress;
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    
    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
    
    public List<OrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItem> orderItems) { this.orderItems = orderItems; }

    public String getPaymentIntentId() { return paymentIntentId; }
    public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
}

enum OrderStatus {
    PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
}