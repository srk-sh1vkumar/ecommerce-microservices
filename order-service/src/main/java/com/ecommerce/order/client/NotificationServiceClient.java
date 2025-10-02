package com.ecommerce.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Feign client for notification-service integration in Order Service.
 * Provides email notification capabilities for order-related events.
 *
 * <p>This client communicates with the notification-service microservice
 * to send order confirmation and shipping notification emails.
 *
 * <p>Features:
 * <ul>
 *   <li>Order confirmation email after successful checkout</li>
 *   <li>Order shipped notification with tracking information</li>
 *   <li>Automatic service discovery via Eureka</li>
 *   <li>Circuit breaker support for resilience</li>
 * </ul>
 *
 * @author E-commerce Development Team
 * @version 1.0
 * @since 1.0
 */
@FeignClient(name = "notification-service")
public interface NotificationServiceClient {

    /**
     * Sends order confirmation email to customer.
     *
     * <p>The order confirmation email includes:
     * <ul>
     *   <li>Order ID and order date</li>
     *   <li>List of ordered items with quantities and prices</li>
     *   <li>Total amount</li>
     *   <li>Shipping address</li>
     *   <li>Estimated delivery date</li>
     * </ul>
     *
     * @param email Customer's email address
     * @param name Customer's full name
     * @param orderId Unique order identifier
     * @param orderTotal Total order amount (formatted)
     * @param orderItems JSON string or formatted list of order items
     * @return ResponseEntity containing success/failure status
     * @throws feign.FeignException if notification service is unavailable
     *
     * @apiNote This method should be called asynchronously after order creation
     * to avoid blocking the checkout response.
     *
     * @implNote Email sending failures are logged but don't affect order processing.
     */
    @PostMapping("/api/notifications/order-confirmation")
    ResponseEntity<Map<String, String>> sendOrderConfirmation(
            @RequestParam("email") String email,
            @RequestParam("name") String name,
            @RequestParam("orderId") String orderId,
            @RequestParam("orderTotal") String orderTotal,
            @RequestParam("orderItems") String orderItems
    );

    /**
     * Sends order shipped notification with tracking information.
     *
     * <p>The shipping notification email includes:
     * <ul>
     *   <li>Order ID</li>
     *   <li>Shipping carrier information</li>
     *   <li>Tracking number</li>
     *   <li>Estimated delivery date</li>
     *   <li>Link to track package</li>
     * </ul>
     *
     * @param email Customer's email address
     * @param name Customer's full name
     * @param orderId Order identifier
     * @param trackingNumber Shipping tracking number
     * @param carrier Shipping carrier name (e.g., "FedEx", "UPS", "USPS")
     * @return ResponseEntity containing success/failure status
     * @throws feign.FeignException if notification service is unavailable
     *
     * @apiNote Called when order status changes to "SHIPPED"
     *
     * @implNote Tracking number should be validated before calling this method
     */
    @PostMapping("/api/notifications/order-shipped")
    ResponseEntity<Map<String, String>> sendOrderShipped(
            @RequestParam("email") String email,
            @RequestParam("name") String name,
            @RequestParam("orderId") String orderId,
            @RequestParam("trackingNumber") String trackingNumber,
            @RequestParam("carrier") String carrier
    );
}
