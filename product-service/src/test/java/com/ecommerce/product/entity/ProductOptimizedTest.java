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

    // Parameterized Constructor Tests

    @Test
    @DisplayName("Parameterized constructor should create product with required fields")
    void parameterizedConstructor_ShouldCreateProductWithRequiredFields() {
        // Arrange
        String name = "iPhone 15";
        String description = "Latest iPhone";
        BigDecimal price = new BigDecimal("999.99");
        String category = "Electronics";
        Integer stock = 50;
        String imageUrl = "https://example.com/iphone.jpg";

        // Act
        ProductOptimized newProduct = new ProductOptimized(name, description, price, category, stock, imageUrl);

        // Assert
        assertThat(newProduct.getName()).isEqualTo(name);
        assertThat(newProduct.getDescription()).isEqualTo(description);
        assertThat(newProduct.getPrice()).isEqualByComparingTo(price);
        assertThat(newProduct.getCategory()).isEqualTo(category);
        assertThat(newProduct.getStockQuantity()).isEqualTo(stock);
        assertThat(newProduct.getImageUrl()).isEqualTo(imageUrl);
        assertThat(newProduct.getCreatedAt()).isNotNull();
        assertThat(newProduct.getUpdatedAt()).isNotNull();
    }

    // Business Method Tests

    @Test
    @DisplayName("isAvailable should return true when product is active and has stock")
    void isAvailable_WhenActiveAndInStock_ShouldReturnTrue() {
        // Arrange
        product.setActive(true);
        product.setStockQuantity(10);

        // Act & Assert
        assertThat(product.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("isAvailable should return false when product is inactive")
    void isAvailable_WhenInactive_ShouldReturnFalse() {
        // Arrange
        product.setActive(false);
        product.setStockQuantity(10);

        // Act & Assert
        assertThat(product.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("isAvailable should return false when stock is zero")
    void isAvailable_WhenNoStock_ShouldReturnFalse() {
        // Arrange
        product.setActive(true);
        product.setStockQuantity(0);

        // Act & Assert
        assertThat(product.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("isAvailable should return false when stock is null")
    void isAvailable_WhenStockIsNull_ShouldReturnFalse() {
        // Arrange
        product.setActive(true);
        product.setStockQuantity(null);

        // Act & Assert
        assertThat(product.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("isLowStock should return true when stock is between 1 and 9")
    void isLowStock_WhenStockBetween1And9_ShouldReturnTrue() {
        // Arrange
        product.setStockQuantity(5);

        // Act & Assert
        assertThat(product.isLowStock()).isTrue();
    }

    @Test
    @DisplayName("isLowStock should return false when stock is 10 or more")
    void isLowStock_WhenStock10OrMore_ShouldReturnFalse() {
        // Arrange
        product.setStockQuantity(10);

        // Act & Assert
        assertThat(product.isLowStock()).isFalse();
    }

    @Test
    @DisplayName("isLowStock should return false when stock is zero")
    void isLowStock_WhenStockIsZero_ShouldReturnFalse() {
        // Arrange
        product.setStockQuantity(0);

        // Act & Assert
        assertThat(product.isLowStock()).isFalse();
    }

    @Test
    @DisplayName("isLowStock should return false when stock is null")
    void isLowStock_WhenStockIsNull_ShouldReturnFalse() {
        // Arrange
        product.setStockQuantity(null);

        // Act & Assert
        assertThat(product.isLowStock()).isFalse();
    }

    @Test
    @DisplayName("decrementStock should reduce stock quantity")
    void decrementStock_WithSufficientStock_ShouldReduceQuantity() {
        // Arrange
        product.setStockQuantity(20);
        LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);

        // Act
        product.decrementStock(5);

        // Assert
        assertThat(product.getStockQuantity()).isEqualTo(15);
        assertThat(product.getUpdatedAt()).isAfter(beforeUpdate);
    }

    @Test
    @DisplayName("decrementStock should throw exception when insufficient stock")
    void decrementStock_WithInsufficientStock_ShouldThrowException() {
        // Arrange
        product.setStockQuantity(3);

        // Act & Assert
        assertThatThrownBy(() -> product.decrementStock(5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Insufficient stock");
    }

    @Test
    @DisplayName("decrementStock should work when decrementing exact stock amount")
    void decrementStock_WithExactStockAmount_ShouldSetToZero() {
        // Arrange
        product.setStockQuantity(10);

        // Act
        product.decrementStock(10);

        // Assert
        assertThat(product.getStockQuantity()).isZero();
    }

    @Test
    @DisplayName("incrementStock should increase stock quantity")
    void incrementStock_ShouldIncreaseQuantity() {
        // Arrange
        product.setStockQuantity(10);
        LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);

        // Act
        product.incrementStock(5);

        // Assert
        assertThat(product.getStockQuantity()).isEqualTo(15);
        assertThat(product.getUpdatedAt()).isAfter(beforeUpdate);
    }

    @Test
    @DisplayName("incrementStock should work from zero stock")
    void incrementStock_FromZero_ShouldSetToQuantity() {
        // Arrange
        product.setStockQuantity(0);

        // Act
        product.incrementStock(20);

        // Assert
        assertThat(product.getStockQuantity()).isEqualTo(20);
    }

    @Test
    @DisplayName("updateRating should set initial rating when null")
    void updateRating_WhenRatingIsNull_ShouldSetInitialRating() {
        // Arrange
        product.setRating(null);
        product.setReviewCount(0);
        LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);

        // Act
        product.updateRating(4.5);

        // Assert
        assertThat(product.getRating()).isEqualTo(4.5);
        assertThat(product.getReviewCount()).isEqualTo(1);
        assertThat(product.getUpdatedAt()).isAfter(beforeUpdate);
    }

    @Test
    @DisplayName("updateRating should calculate average rating")
    void updateRating_WithExistingRating_ShouldCalculateAverage() {
        // Arrange
        product.setRating(4.0);
        product.setReviewCount(2);

        // Act
        product.updateRating(5.0);

        // Assert
        // (4.0 * 2 + 5.0) / 3 = 13.0 / 3 = 4.333...
        assertThat(product.getRating()).isCloseTo(4.33, within(0.01));
        assertThat(product.getReviewCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("updateRating should handle multiple ratings correctly")
    void updateRating_WithMultipleRatings_ShouldMaintainCorrectAverage() {
        // Arrange
        product.setRating(null);

        // Act
        product.updateRating(5.0);
        product.updateRating(4.0);
        product.updateRating(3.0);

        // Assert
        // (5.0 + 4.0 + 3.0) / 3 = 4.0
        assertThat(product.getRating()).isEqualTo(4.0);
        assertThat(product.getReviewCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("incrementViews should increase view counter")
    void incrementViews_ShouldIncreaseCounter() {
        // Arrange
        product.setViews(100L);

        // Act
        product.incrementViews();

        // Assert
        assertThat(product.getViews()).isEqualTo(101L);
    }

    @Test
    @DisplayName("incrementViews should work from zero views")
    void incrementViews_FromZero_ShouldSetToOne() {
        // Arrange
        product.setViews(0L);

        // Act
        product.incrementViews();

        // Assert
        assertThat(product.getViews()).isEqualTo(1L);
    }

    // Setter with Timestamp Update Tests

    @Test
    @DisplayName("setName should update updatedAt timestamp")
    void setName_ShouldUpdateTimestamp() {
        // Arrange
        LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);

        // Act
        product.setName("New Name");

        // Assert
        assertThat(product.getUpdatedAt()).isAfter(beforeUpdate);
    }

    @Test
    @DisplayName("setDescription should update updatedAt timestamp")
    void setDescription_ShouldUpdateTimestamp() {
        // Arrange
        LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);

        // Act
        product.setDescription("New Description");

        // Assert
        assertThat(product.getUpdatedAt()).isAfter(beforeUpdate);
    }

    @Test
    @DisplayName("setPrice should update updatedAt timestamp")
    void setPrice_ShouldUpdateTimestamp() {
        // Arrange
        LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);

        // Act
        product.setPrice(new BigDecimal("99.99"));

        // Assert
        assertThat(product.getUpdatedAt()).isAfter(beforeUpdate);
    }

    @Test
    @DisplayName("setCategory should update updatedAt timestamp")
    void setCategory_ShouldUpdateTimestamp() {
        // Arrange
        LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);

        // Act
        product.setCategory("New Category");

        // Assert
        assertThat(product.getUpdatedAt()).isAfter(beforeUpdate);
    }

    @Test
    @DisplayName("setStockQuantity should update updatedAt timestamp")
    void setStockQuantity_ShouldUpdateTimestamp() {
        // Arrange
        LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);

        // Act
        product.setStockQuantity(50);

        // Assert
        assertThat(product.getUpdatedAt()).isAfter(beforeUpdate);
    }

    // Additional Field Tests

    @Test
    @DisplayName("Should set and get brand")
    void setBrand_ShouldUpdateBrand() {
        // Arrange
        String brand = "Apple";

        // Act
        product.setBrand(brand);

        // Assert
        assertThat(product.getBrand()).isEqualTo(brand);
    }

    @Test
    @DisplayName("Should set and get rating")
    void setRating_ShouldUpdateRating() {
        // Arrange
        Double rating = 4.5;

        // Act
        product.setRating(rating);

        // Assert
        assertThat(product.getRating()).isEqualTo(rating);
    }

    @Test
    @DisplayName("Should set and get review count")
    void setReviewCount_ShouldUpdateReviewCount() {
        // Arrange
        Integer reviewCount = 100;

        // Act
        product.setReviewCount(reviewCount);

        // Assert
        assertThat(product.getReviewCount()).isEqualTo(reviewCount);
    }

    @Test
    @DisplayName("Should set and get views")
    void setViews_ShouldUpdateViews() {
        // Arrange
        Long views = 5000L;

        // Act
        product.setViews(views);

        // Assert
        assertThat(product.getViews()).isEqualTo(views);
    }

    @Test
    @DisplayName("Should set and get featured")
    void setFeatured_ShouldUpdateFeatured() {
        // Arrange
        Boolean featured = true;

        // Act
        product.setFeatured(featured);

        // Assert
        assertThat(product.getFeatured()).isTrue();
    }

    @Test
    @DisplayName("Should set and get active")
    void setActive_ShouldUpdateActive() {
        // Arrange
        Boolean active = false;

        // Act
        product.setActive(active);

        // Assert
        assertThat(product.getActive()).isFalse();
    }

    // equals, hashCode, toString Tests

    @Test
    @DisplayName("equals should return true for same object")
    void equals_SameObject_ShouldReturnTrue() {
        // Act & Assert
        assertThat(product).isEqualTo(product);
    }

    @Test
    @DisplayName("equals should return true for objects with same ID")
    void equals_SameId_ShouldReturnTrue() {
        // Arrange
        product.setId("prod123");
        ProductOptimized other = new ProductOptimized();
        other.setId("prod123");

        // Act & Assert
        assertThat(product).isEqualTo(other);
    }

    @Test
    @DisplayName("equals should return false for objects with different IDs")
    void equals_DifferentId_ShouldReturnFalse() {
        // Arrange
        product.setId("prod123");
        ProductOptimized other = new ProductOptimized();
        other.setId("prod456");

        // Act & Assert
        assertThat(product).isNotEqualTo(other);
    }

    @Test
    @DisplayName("equals should return false for null")
    void equals_Null_ShouldReturnFalse() {
        // Act & Assert
        assertThat(product).isNotEqualTo(null);
    }

    @Test
    @DisplayName("equals should return false for different class")
    void equals_DifferentClass_ShouldReturnFalse() {
        // Act & Assert
        assertThat(product).isNotEqualTo("not a product");
    }

    @Test
    @DisplayName("hashCode should be same for objects with same ID")
    void hashCode_SameId_ShouldBeSame() {
        // Arrange
        product.setId("prod123");
        ProductOptimized other = new ProductOptimized();
        other.setId("prod123");

        // Act & Assert
        assertThat(product.hashCode()).isEqualTo(other.hashCode());
    }

    @Test
    @DisplayName("hashCode should be different for objects with different IDs")
    void hashCode_DifferentId_ShouldBeDifferent() {
        // Arrange
        product.setId("prod123");
        ProductOptimized other = new ProductOptimized();
        other.setId("prod456");

        // Act & Assert
        assertThat(product.hashCode()).isNotEqualTo(other.hashCode());
    }

    @Test
    @DisplayName("toString should contain key product information")
    void toString_ShouldContainKeyInformation() {
        // Arrange
        product.setId("prod123");
        product.setName("Test Product");
        product.setCategory("Electronics");
        product.setPrice(new BigDecimal("99.99"));
        product.setStockQuantity(50);

        // Act
        String result = product.toString();

        // Assert
        assertThat(result).contains("prod123");
        assertThat(result).contains("Test Product");
        assertThat(result).contains("Electronics");
        assertThat(result).contains("99.99");
        assertThat(result).contains("50");
    }

    @Test
    @DisplayName("Default values should be set correctly")
    void defaultValues_ShouldBeSetCorrectly() {
        // Arrange & Act
        ProductOptimized newProduct = new ProductOptimized();

        // Assert
        assertThat(newProduct.getReviewCount()).isZero();
        assertThat(newProduct.getViews()).isZero();
        assertThat(newProduct.getFeatured()).isFalse();
        assertThat(newProduct.getActive()).isTrue();
        assertThat(newProduct.getCreatedAt()).isNotNull();
        assertThat(newProduct.getUpdatedAt()).isNotNull();
    }
}
