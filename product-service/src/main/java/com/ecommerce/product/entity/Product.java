package com.ecommerce.product.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.TextIndexed;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "products")
@CompoundIndexes({
    @CompoundIndex(name = "category_price_idx", def = "{'category': 1, 'price': 1}"),
    @CompoundIndex(name = "category_stock_idx", def = "{'category': 1, 'stockQuantity': 1}"),
    @CompoundIndex(name = "category_created_idx", def = "{'category': 1, 'createdAt': -1}")
})
public class Product {
    @Id
    private String id;

    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 100, message = "Product name must be between 3 and 100 characters")
    @TextIndexed
    private String name;

    @NotBlank(message = "Product description is required")
    @Size(min = 10, max = 500, message = "Description must be between 10 and 500 characters")
    @TextIndexed
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @DecimalMax(value = "999999.99", message = "Price is too high")
    private BigDecimal price;

    @NotBlank(message = "Category is required")
    @Pattern(regexp = "^(Electronics|Clothing|Books|Home|Toys|Sports|Beauty|Food)$",
             message = "Invalid category. Must be one of: Electronics, Clothing, Books, Home, Toys, Sports, Beauty, Food")
    @Indexed
    private String category;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Max(value = 10000, message = "Stock quantity is too high")
    private Integer stockQuantity;

    @Pattern(regexp = "^(https?://.*|)$", message = "Image URL must be a valid HTTP/HTTPS URL or empty")
    private String imageUrl;

    @Indexed
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public Product() {}
    
    public Product(String name, String description, BigDecimal price, String category, Integer stockQuantity, String imageUrl) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.stockQuantity = stockQuantity;
        this.imageUrl = imageUrl;
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}