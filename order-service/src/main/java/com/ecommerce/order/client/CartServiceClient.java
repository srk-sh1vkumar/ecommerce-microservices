package com.ecommerce.order.client;

import com.ecommerce.order.dto.CartItemDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "cart-service")
public interface CartServiceClient {
    
    @GetMapping("/api/cart/{userEmail}")
    List<CartItemDTO> getCartItems(@PathVariable("userEmail") String userEmail);
    
    @DeleteMapping("/api/cart/{userEmail}")
    void clearCart(@PathVariable("userEmail") String userEmail);
}