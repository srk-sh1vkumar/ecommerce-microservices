package com.ecommerce.order.controller;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.service.OrderService;
import com.ecommerce.order.dto.CheckoutRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Order Controller with comprehensive API documentation.
 * Manages order processing and order history.
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order Management", description = "APIs for order processing, checkout, and order history")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @Operation(
            summary = "Checkout Cart",
            description = "Processes checkout for user's cart. Creates order, updates inventory, and clears cart. Requires authentication.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order created successfully",
                    content = @Content(schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request, empty cart, or insufficient stock"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    @PostMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Order> checkout(
            @Parameter(description = "Checkout request with user email and shipping address", required = true)
            @jakarta.validation.Valid @RequestBody CheckoutRequest request) {
        try {
            Order order = orderService.checkout(request);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Operation(
            summary = "Get Order History",
            description = "Retrieves all orders for a specific user, sorted by date (newest first). Requires authentication.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order history retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Order.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    @GetMapping("/history/{userEmail}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Order>> getOrderHistory(
            @Parameter(description = "User email", required = true, example = "user@example.com")
            @PathVariable String userEmail) {
        List<Order> orders = orderService.getOrderHistory(userEmail);
        return ResponseEntity.ok(orders);
    }
    
    @Operation(
            summary = "Get Order by ID",
            description = "Retrieves detailed information about a specific order. Requires authentication.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order found",
                    content = @Content(schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Order> getOrderById(
            @Parameter(description = "Order ID", required = true, example = "507f1f77bcf86cd799439011")
            @PathVariable String orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}