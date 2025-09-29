package com.ecommerce.cart.service;

import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartItemRepository;
import com.ecommerce.cart.dto.ProductDTO;
import com.ecommerce.cart.dto.AddToCartRequest;
import com.ecommerce.cart.client.ProductServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    public List<CartItem> getCartItems(String userEmail) {
        return cartItemRepository.findByUserEmail(userEmail);
    }
    
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
            return cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem(
                    request.getUserEmail(),
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    request.getQuantity()
            );
            return cartItemRepository.save(newItem);
        }
    }
    
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
    
    @Transactional
    public void removeFromCart(String userEmail, String productId) {
        cartItemRepository.deleteByUserEmailAndProductId(userEmail, productId);
    }
    
    @Transactional
    public void clearCart(String userEmail) {
        cartItemRepository.deleteByUserEmail(userEmail);
    }
    
    public BigDecimal getCartTotal(String userEmail) {
        List<CartItem> items = cartItemRepository.findByUserEmail(userEmail);
        return items.stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}