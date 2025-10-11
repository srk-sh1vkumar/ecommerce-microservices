package com.ecommerce.cart.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ProductDTO.
 */
@DisplayName("ProductDTO Tests")
class ProductDTOTest {

    @Test
    @DisplayName("Default constructor should create empty DTO")
    void defaultConstructor_ShouldCreateEmptyDTO() {
        ProductDTO productDTO = new ProductDTO();

        assertThat(productDTO.getId()).isNull();
        assertThat(productDTO.getName()).isNull();
        assertThat(productDTO.getDescription()).isNull();
        assertThat(productDTO.getPrice()).isNull();
        assertThat(productDTO.getCategory()).isNull();
        assertThat(productDTO.getStockQuantity()).isNull();
        assertThat(productDTO.getImageUrl()).isNull();
    }

    @Test
    @DisplayName("Should set and get all fields")
    void setAllFields_ShouldWork() {
        ProductDTO productDTO = new ProductDTO();
        productDTO.setId("prod123");
        productDTO.setName("Laptop");
        productDTO.setDescription("High-performance laptop");
        productDTO.setPrice(new BigDecimal("1299.99"));
        productDTO.setCategory("Electronics");
        productDTO.setStockQuantity(50);
        productDTO.setImageUrl("http://example.com/laptop.jpg");

        assertThat(productDTO.getId()).isEqualTo("prod123");
        assertThat(productDTO.getName()).isEqualTo("Laptop");
        assertThat(productDTO.getDescription()).isEqualTo("High-performance laptop");
        assertThat(productDTO.getPrice()).isEqualTo(new BigDecimal("1299.99"));
        assertThat(productDTO.getCategory()).isEqualTo("Electronics");
        assertThat(productDTO.getStockQuantity()).isEqualTo(50);
        assertThat(productDTO.getImageUrl()).isEqualTo("http://example.com/laptop.jpg");
    }

    @Test
    @DisplayName("Should handle null values")
    void setNullValues_ShouldAcceptNulls() {
        ProductDTO productDTO = new ProductDTO();
        productDTO.setId(null);
        productDTO.setName(null);
        productDTO.setDescription(null);
        productDTO.setPrice(null);
        productDTO.setCategory(null);
        productDTO.setStockQuantity(null);
        productDTO.setImageUrl(null);

        assertThat(productDTO.getId()).isNull();
        assertThat(productDTO.getName()).isNull();
        assertThat(productDTO.getDescription()).isNull();
        assertThat(productDTO.getPrice()).isNull();
        assertThat(productDTO.getCategory()).isNull();
        assertThat(productDTO.getStockQuantity()).isNull();
        assertThat(productDTO.getImageUrl()).isNull();
    }
}
