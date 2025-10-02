package com.ecommerce.wishlist.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WishlistItem {
    private String productId;
    private String productName;
    private BigDecimal productPrice;
    private String imageUrl;
    private LocalDateTime addedAt;
    private Boolean inStock;

    public WishlistItem() {
    }

    public WishlistItem(String productId, String productName, BigDecimal productPrice, String imageUrl) {
        this.productId = productId;
        this.productName = productName;
        this.productPrice = productPrice;
        this.imageUrl = imageUrl;
        this.addedAt = LocalDateTime.now();
        this.inStock = true;
    }

    // Getters and Setters
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(BigDecimal productPrice) {
        this.productPrice = productPrice;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }

    public Boolean getInStock() {
        return inStock;
    }

    public void setInStock(Boolean inStock) {
        this.inStock = inStock;
    }
}
