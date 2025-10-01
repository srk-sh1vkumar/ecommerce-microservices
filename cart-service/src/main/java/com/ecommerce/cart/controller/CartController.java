package com.ecommerce.cart.controller;

import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.service.CartService;
import com.ecommerce.cart.dto.AddToCartRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    
    @Autowired
    private CartService cartService;
    
    @GetMapping("/{userEmail}")
    public ResponseEntity<List<CartItem>> getCartItems(@PathVariable String userEmail) {
        List<CartItem> items = cartService.getCartItems(userEmail);
        return ResponseEntity.ok(items);
    }
    
    @PostMapping("/add")
    public ResponseEntity<CartItem> addToCart(@jakarta.validation.Valid @RequestBody AddToCartRequest request) {
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
    
    @DeleteMapping("/{userEmail}")
    public ResponseEntity<Void> clearCart(@PathVariable String userEmail) {
        cartService.clearCart(userEmail);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{userEmail}/total")
    public ResponseEntity<BigDecimal> getCartTotal(@PathVariable String userEmail) {
        BigDecimal total = cartService.getCartTotal(userEmail);
        return ResponseEntity.ok(total);
    }
}