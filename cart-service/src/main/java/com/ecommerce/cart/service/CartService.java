package com.ecommerce.cart.service;

import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartItemRepository;
import com.ecommerce.cart.dto.ProductDTO;
import com.ecommerce.cart.dto.AddToCartRequest;
import com.ecommerce.cart.client.ProductServiceClient;
import com.ecommerce.common.metrics.MetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductServiceClient productServiceClient;

    @Autowired
    private MetricsService metricsService;
    
    @Cacheable(value = "userCartItems", key = "#userEmail")
    public List<CartItem> getCartItems(String userEmail) {
        return cartItemRepository.findByUserEmail(userEmail);
    }
    
    @CacheEvict(value = {"userCartItems", "cartItemCount"}, key = "#request.userEmail")
    public CartItem addToCart(AddToCartRequest request) {
        ProductDTO product = productServiceClient.getProductById(request.getProductId());
        
        if (product == null) {
            throw new RuntimeException("Product not found");
        }
        
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new RuntimeException("Insufficient stock");
        }
        
        Optional<CartItem> existingItem = cartItemRepository.findByUserEmailAndProductId(
                request.getUserEmail(), request.getProductId());
        
        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            metricsService.incrementItemsAddedToCart();
            return cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem(
                    request.getUserEmail(),
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    request.getQuantity()
            );
            metricsService.incrementItemsAddedToCart();
            return cartItemRepository.save(newItem);
        }
    }
    
    @CacheEvict(value = {"userCartItems", "cartItemCount"}, key = "#userEmail")
    public CartItem updateCartItemQuantity(String userEmail, String productId, Integer quantity) {
        CartItem item = cartItemRepository.findByUserEmailAndProductId(userEmail, productId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));
        
        if (quantity <= 0) {
            cartItemRepository.delete(item);
            return null;
        }
        
        item.setQuantity(quantity);
        return cartItemRepository.save(item);
    }
    
    @CacheEvict(value = {"userCartItems", "cartItemCount"}, key = "#userEmail")
    @Transactional
    public void removeFromCart(String userEmail, String productId) {
        cartItemRepository.deleteByUserEmailAndProductId(userEmail, productId);
        metricsService.incrementItemsRemovedFromCart();
    }
    
    @CacheEvict(value = {"userCartItems", "cartItemCount"}, key = "#userEmail")
    @Transactional
    public void clearCart(String userEmail) {
        cartItemRepository.deleteByUserEmail(userEmail);
        metricsService.incrementCartsCleared();
    }
    
    public BigDecimal getCartTotal(String userEmail) {
        List<CartItem> items = cartItemRepository.findByUserEmail(userEmail);
        return items.stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}