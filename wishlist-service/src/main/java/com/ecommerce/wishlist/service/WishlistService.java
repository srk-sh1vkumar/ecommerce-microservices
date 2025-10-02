package com.ecommerce.wishlist.service;

import com.ecommerce.wishlist.client.ProductServiceClient;
import com.ecommerce.wishlist.dto.ProductDTO;
import com.ecommerce.wishlist.entity.Wishlist;
import com.ecommerce.wishlist.entity.WishlistItem;
import com.ecommerce.wishlist.repository.WishlistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing user wishlists.
 * Allows users to save products for later purchase.
 */
@Service
public class WishlistService {

    private static final Logger logger = LoggerFactory.getLogger(WishlistService.class);

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private ProductServiceClient productServiceClient;

    /**
     * Get user's wishlist with cached results.
     *
     * @param userEmail User's email
     * @return User's wishlist
     */
    @Cacheable(value = "wishlist", key = "#userEmail")
    public Wishlist getWishlist(String userEmail) {
        logger.debug("Fetching wishlist for user: {}", userEmail);
        return wishlistRepository.findByUserEmail(userEmail)
                .orElseGet(() -> new Wishlist(userEmail));
    }

    /**
     * Add a product to user's wishlist.
     *
     * @param userEmail User's email
     * @param productId Product ID to add
     * @return Updated wishlist
     */
    @CacheEvict(value = "wishlist", key = "#userEmail")
    @Transactional
    public Wishlist addToWishlist(String userEmail, String productId) {
        logger.info("Adding product {} to wishlist for user {}", productId, userEmail);

        // Fetch product details
        ProductDTO product;
        try {
            product = productServiceClient.getProductById(productId);
        } catch (Exception e) {
            logger.error("Failed to fetch product details for {}", productId, e);
            throw new RuntimeException("Product not found: " + productId);
        }

        // Get or create wishlist
        Wishlist wishlist = wishlistRepository.findByUserEmail(userEmail)
                .orElseGet(() -> new Wishlist(userEmail));

        // Check if product already in wishlist
        boolean exists = wishlist.getItems().stream()
                .anyMatch(item -> item.getProductId().equals(productId));

        if (exists) {
            logger.warn("Product {} already in wishlist for user {}", productId, userEmail);
            throw new RuntimeException("Product already in wishlist");
        }

        // Add product to wishlist
        WishlistItem item = new WishlistItem(
                productId,
                product.getName(),
                product.getPrice(),
                product.getImageUrl()
        );
        item.setInStock(product.getStockQuantity() != null && product.getStockQuantity() > 0);

        wishlist.getItems().add(item);
        wishlist.setUpdatedAt(LocalDateTime.now());

        Wishlist savedWishlist = wishlistRepository.save(wishlist);
        logger.info("Product {} added to wishlist for user {}", productId, userEmail);

        return savedWishlist;
    }

    /**
     * Remove a product from user's wishlist.
     *
     * @param userEmail User's email
     * @param productId Product ID to remove
     * @return Updated wishlist
     */
    @CacheEvict(value = "wishlist", key = "#userEmail")
    @Transactional
    public Wishlist removeFromWishlist(String userEmail, String productId) {
        logger.info("Removing product {} from wishlist for user {}", productId, userEmail);

        Wishlist wishlist = wishlistRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Wishlist not found for user: " + userEmail));

        boolean removed = wishlist.getItems().removeIf(item -> item.getProductId().equals(productId));

        if (!removed) {
            logger.warn("Product {} not found in wishlist for user {}", productId, userEmail);
            throw new RuntimeException("Product not found in wishlist");
        }

        wishlist.setUpdatedAt(LocalDateTime.now());
        Wishlist savedWishlist = wishlistRepository.save(wishlist);

        logger.info("Product {} removed from wishlist for user {}", productId, userEmail);
        return savedWishlist;
    }

    /**
     * Clear all items from user's wishlist.
     *
     * @param userEmail User's email
     */
    @CacheEvict(value = "wishlist", key = "#userEmail")
    @Transactional
    public void clearWishlist(String userEmail) {
        logger.info("Clearing wishlist for user {}", userEmail);

        Wishlist wishlist = wishlistRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Wishlist not found for user: " + userEmail));

        wishlist.getItems().clear();
        wishlist.setUpdatedAt(LocalDateTime.now());
        wishlistRepository.save(wishlist);

        logger.info("Wishlist cleared for user {}", userEmail);
    }

    /**
     * Get count of items in user's wishlist.
     *
     * @param userEmail User's email
     * @return Number of items in wishlist
     */
    @Cacheable(value = "wishlistCount", key = "#userEmail")
    public int getWishlistItemCount(String userEmail) {
        return wishlistRepository.findByUserEmail(userEmail)
                .map(Wishlist::getItemCount)
                .orElse(0);
    }

    /**
     * Check if a product is in user's wishlist.
     *
     * @param userEmail User's email
     * @param productId Product ID
     * @return true if product is in wishlist
     */
    public boolean isInWishlist(String userEmail, String productId) {
        return wishlistRepository.findByUserEmail(userEmail)
                .map(wishlist -> wishlist.getItems().stream()
                        .anyMatch(item -> item.getProductId().equals(productId)))
                .orElse(false);
    }

    /**
     * Move all wishlist items to cart.
     *
     * @param userEmail User's email
     * @return List of product IDs moved to cart
     */
    @CacheEvict(value = "wishlist", key = "#userEmail")
    @Transactional
    public List<String> moveToCart(String userEmail) {
        logger.info("Moving wishlist items to cart for user {}", userEmail);

        Wishlist wishlist = wishlistRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Wishlist not found for user: " + userEmail));

        List<String> productIds = wishlist.getItems().stream()
                .map(WishlistItem::getProductId)
                .toList();

        // Clear wishlist after moving to cart
        wishlist.getItems().clear();
        wishlist.setUpdatedAt(LocalDateTime.now());
        wishlistRepository.save(wishlist);

        logger.info("Moved {} items from wishlist to cart for user {}", productIds.size(), userEmail);
        return productIds;
    }

    /**
     * Update stock status for all items in wishlist.
     * Should be called periodically or when viewing wishlist.
     *
     * @param userEmail User's email
     */
    @CacheEvict(value = "wishlist", key = "#userEmail")
    public void refreshStockStatus(String userEmail) {
        logger.debug("Refreshing stock status for wishlist: {}", userEmail);

        wishlistRepository.findByUserEmail(userEmail).ifPresent(wishlist -> {
            wishlist.getItems().forEach(item -> {
                try {
                    ProductDTO product = productServiceClient.getProductById(item.getProductId());
                    item.setInStock(product.getStockQuantity() != null && product.getStockQuantity() > 0);
                    item.setProductPrice(product.getPrice());
                } catch (Exception e) {
                    logger.warn("Failed to refresh product {}", item.getProductId(), e);
                    item.setInStock(false);
                }
            });

            wishlist.setUpdatedAt(LocalDateTime.now());
            wishlistRepository.save(wishlist);
        });
    }
}
