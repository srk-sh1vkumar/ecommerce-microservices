package com.ecommerce.wishlist.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB entity representing a user's product wishlist.
 *
 * <p>This entity stores a user's saved products for later purchase consideration.
 * Each user can have exactly one wishlist containing multiple {@link WishlistItem}s.</p>
 *
 * <p><b>Database Schema:</b></p>
 * <ul>
 *   <li><b>Collection:</b> wishlists</li>
 *   <li><b>Primary Key:</b> id (auto-generated MongoDB ObjectId)</li>
 *   <li><b>Unique Index:</b> userEmail (ensures one wishlist per user)</li>
 *   <li><b>Embedded Documents:</b> items (List of WishlistItem)</li>
 * </ul>
 *
 * <p><b>Automatic Timestamps:</b></p>
 * <ul>
 *   <li>{@code createdAt} - Set automatically on creation</li>
 *   <li>{@code updatedAt} - Should be updated on every modification</li>
 * </ul>
 *
 * <p><b>Example JSON:</b></p>
 * <pre>{@code
 * {
 *   "id": "507f1f77bcf86cd799439011",
 *   "userEmail": "user@example.com",
 *   "items": [
 *     {
 *       "productId": "prod-123",
 *       "productName": "Wireless Mouse",
 *       "productPrice": 29.99,
 *       "imageUrl": "https://example.com/image.jpg",
 *       "addedAt": "2025-10-01T10:30:00",
 *       "inStock": true
 *     }
 *   ],
 *   "createdAt": "2025-10-01T10:00:00",
 *   "updatedAt": "2025-10-01T10:30:00"
 * }
 * }</pre>
 *
 * @author E-commerce Development Team
 * @version 1.0
 * @since 1.0
 * @see WishlistItem
 * @see com.ecommerce.wishlist.repository.WishlistRepository
 */
@Document(collection = "wishlists")
public class Wishlist {

    /**
     * Unique identifier for the wishlist (MongoDB ObjectId).
     */
    @Id
    private String id;

    /**
     * User's email address - unique index ensures one wishlist per user.
     */
    @Indexed(unique = true)
    private String userEmail;

    /**
     * List of wishlist items (embedded documents).
     * Initialized as empty ArrayList to avoid null checks.
     */
    private List<WishlistItem> items = new ArrayList<>();

    /**
     * Timestamp when the wishlist was created.
     * Automatically set in constructor.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the wishlist was last updated.
     * Should be updated whenever items are added/removed.
     */
    private LocalDateTime updatedAt;

    public Wishlist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Wishlist(String userEmail) {
        this();
        this.userEmail = userEmail;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public List<WishlistItem> getItems() {
        return items;
    }

    public void setItems(List<WishlistItem> items) {
        this.items = items;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getItemCount() {
        return items != null ? items.size() : 0;
    }
}
