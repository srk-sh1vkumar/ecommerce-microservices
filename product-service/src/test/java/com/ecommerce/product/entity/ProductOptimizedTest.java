package com.ecommerce.product.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ProductOptimized entity.
 * Tests optimized entity construction, getters, setters, and business logic.
 */
@DisplayName("ProductOptimized Entity Tests")
class ProductOptimizedTest {

    private ProductOptimized product;

    @BeforeEach
    void setUp() {
        product = new ProductOptimized();
    }

    @Test
    @DisplayName("Default constructor should create product with createdAt")
    void defaultConstructor_ShouldCreateProductWithCreatedAt() {
        // Act
        ProductOptimized newProduct = new ProductOptimized();

        // Assert
        assertThat(newProduct).isNotNull();
        assertThat(newProduct.getCreatedAt()).isNotNull();
        assertThat(newProduct.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should set and get id")
    void setId_ShouldUpdateId() {
        // Arrange
        String id = "prod123";

        // Act
        product.setId(id);

        // Assert
        assertThat(product.getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("Should set and get name")
    void setName_ShouldUpdateName() {
        // Arrange
        String name = "Laptop";

        // Act
        product.setName(name);

        // Assert
        assertThat(product.getName()).isEqualTo(name);
    }

    @Test
    @DisplayName("Should set and get description")
    void setDescription_ShouldUpdateDescription() {
        // Arrange
        String description = "High-performance laptop";

        // Act
        product.setDescription(description);

        // Assert
        assertThat(product.getDescription()).isEqualTo(description);
    }

    @Test
    @DisplayName("Should set and get price")
    void setPrice_ShouldUpdatePrice() {
        // Arrange
        BigDecimal price = new BigDecimal("1299.99");

        // Act
        product.setPrice(price);

        // Assert
        assertThat(product.getPrice()).isEqualByComparingTo(price);
    }

    @Test
    @DisplayName("Should set and get category")
    void setCategory_ShouldUpdateCategory() {
        // Arrange
        String category = "Electronics";

        // Act
        product.setCategory(category);

        // Assert
        assertThat(product.getCategory()).isEqualTo(category);
    }

    @Test
    @DisplayName("Should set and get stock quantity")
    void setStockQuantity_ShouldUpdateStockQuantity() {
        // Arrange
        Integer stock = 100;

        // Act
        product.setStockQuantity(stock);

        // Assert
        assertThat(product.getStockQuantity()).isEqualTo(stock);
    }

    @Test
    @DisplayName("Should set and get image URL")
    void setImageUrl_ShouldUpdateImageUrl() {
        // Arrange
        String imageUrl = "https://cdn.example.com/product.jpg";

        // Act
        product.setImageUrl(imageUrl);

        // Assert
        assertThat(product.getImageUrl()).isEqualTo(imageUrl);
    }

    @Test
    @DisplayName("Should set and get createdAt")
    void setCreatedAt_ShouldUpdateCreatedAt() {
        // Arrange
        LocalDateTime createdAt = LocalDateTime.of(2025, 1, 1, 12, 0);

        // Act
        product.setCreatedAt(createdAt);

        // Assert
        assertThat(product.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("Should set and get updatedAt")
    void setUpdatedAt_ShouldUpdateUpdatedAt() {
        // Arrange
        LocalDateTime updatedAt = LocalDateTime.of(2025, 1, 2, 12, 0);

        // Act
        product.setUpdatedAt(updatedAt);

        // Assert
        assertThat(product.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("Should handle null values for optional fields")
    void setOptionalFields_WithNull_ShouldAccept() {
        // Act
        product.setId(null);
        product.setImageUrl(null);
        product.setUpdatedAt(null);

        // Assert
        assertThat(product.getId()).isNull();
        assertThat(product.getImageUrl()).isNull();
        assertThat(product.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("Should create product with all fields set correctly")
    void fullProduct_ShouldHaveAllFieldsSet() {
        // Arrange & Act
        product.setId("prod456");
        product.setName("Wireless Mouse");
        product.setDescription("Ergonomic wireless mouse");
        product.setPrice(new BigDecimal("29.99"));
        product.setCategory("Electronics");
        product.setStockQuantity(200);
        product.setImageUrl("https://example.com/mouse.jpg");
        LocalDateTime now = LocalDateTime.now();
        product.setCreatedAt(now);
        product.setUpdatedAt(now);

        // Assert
        assertThat(product.getId()).isEqualTo("prod456");
        assertThat(product.getName()).isEqualTo("Wireless Mouse");
        assertThat(product.getDescription()).isEqualTo("Ergonomic wireless mouse");
        assertThat(product.getPrice()).isEqualByComparingTo(new BigDecimal("29.99"));
        assertThat(product.getCategory()).isEqualTo("Electronics");
        assertThat(product.getStockQuantity()).isEqualTo(200);
        assertThat(product.getImageUrl()).isEqualTo("https://example.com/mouse.jpg");
        assertThat(product.getCreatedAt()).isEqualTo(now);
        assertThat(product.getUpdatedAt()).isEqualTo(now);
    }
}
