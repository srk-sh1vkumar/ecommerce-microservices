package com.ecommerce.cart.controller;

import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.service.CartService;
import com.ecommerce.cart.dto.AddToCartRequest;
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
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cart Controller with comprehensive API documentation.
 * Manages shopping cart operations for users.
 */
@RestController
@RequestMapping("/api/cart")
@Tag(name = "Shopping Cart", description = "APIs for managing user shopping carts")
public class CartController {
    
    @Autowired
    private CartService cartService;
    
    @Operation(
            summary = "Get Cart Items",
            description = "Retrieves all items in the user's shopping cart"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart items retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CartItem.class))))
    })
    @GetMapping("/{userEmail}")
    public ResponseEntity<List<CartItem>> getCartItems(
            @Parameter(description = "User email", required = true, example = "user@example.com")
            @PathVariable String userEmail) {
        List<CartItem> items = cartService.getCartItems(userEmail);
        return ResponseEntity.ok(items);
    }
    
    @Operation(
            summary = "Add Item to Cart",
            description = "Adds a product to the user's shopping cart or updates quantity if already exists"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item added to cart successfully",
                    content = @Content(schema = @Schema(implementation = CartItem.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or insufficient stock"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PostMapping("/add")
    public ResponseEntity<CartItem> addToCart(
            @Parameter(description = "Cart item details", required = true)
            @jakarta.validation.Valid @RequestBody AddToCartRequest request) {
        try {
            CartItem item = cartService.addToCart(request);
            return ResponseEntity.ok(item);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{userEmail}/{productId}")
    public ResponseEntity<CartItem> updateCartItemQuantity(
            @PathVariable String userEmail,
            @PathVariable String productId,
            @jakarta.validation.constraints.Min(value = 1, message = "Quantity must be at least 1")
            @jakarta.validation.constraints.Max(value = 100, message = "Quantity cannot exceed 100")
            @RequestParam Integer quantity) {
        try {
            CartItem item = cartService.updateCartItemQuantity(userEmail, productId, quantity);
            return ResponseEntity.ok(item);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{userEmail}/{productId}")
    public ResponseEntity<Void> removeFromCart(
            @PathVariable String userEmail,
            @PathVariable String productId) {
        cartService.removeFromCart(userEmail, productId);
        return ResponseEntity.ok().build();
    }
    
    @Operation(
            summary = "Clear Cart",
            description = "Removes all items from the user's shopping cart"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart cleared successfully")
    })
    @DeleteMapping("/{userEmail}")
    public ResponseEntity<Void> clearCart(
            @Parameter(description = "User email", required = true)
            @PathVariable String userEmail) {
        cartService.clearCart(userEmail);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{userEmail}/total")
    public ResponseEntity<BigDecimal> getCartTotal(@PathVariable String userEmail) {
        BigDecimal total = cartService.getCartTotal(userEmail);
        return ResponseEntity.ok(total);
    }
}