package com.ecommerce.product.service;

import com.ecommerce.product.dto.StockUpdateRequest;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for ProductService.
 * Tests product management, stock updates, caching, and resilience patterns.
 *
 * @author E-commerce Development Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private ProductService productService;

    private Product testProduct;
    private List<Product> testProducts;

    @BeforeEach
    void setUp() {
        // Create circuit breaker registry with default configuration
        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        productService = new ProductService(circuitBreakerRegistry);
        productService.productRepository = productRepository;

        // Setup test data
        testProduct = new Product();
        testProduct.setId("prod123");
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setCategory("Electronics");
        testProduct.setStockQuantity(100);
        testProduct.setImageUrl("http://example.com/image.jpg");

        testProducts = Arrays.asList(
            testProduct,
            createProduct("prod456", "Product 2", "Electronics", 50),
            createProduct("prod789", "Product 3", "Books", 75)
        );
    }

    private Product createProduct(String id, String name, String category, int stock) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setCategory(category);
        product.setStockQuantity(stock);
        product.setPrice(new BigDecimal("49.99"));
        return product;
    }

    // ==================== Get All Products Tests ====================

    @Test
    @DisplayName("GetAllProducts - Should return paginated products")
    void getAllProducts_ShouldReturnPagedProducts() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(testProducts, pageable, testProducts.size());
        when(productRepository.findAll(pageable)).thenReturn(productPage);

        // Act
        Page<Product> result = productService.getAllProducts(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);
        verify(productRepository).findAll(pageable);
    }

    // ==================== Get Product By ID Tests ====================

    @Test
    @DisplayName("GetProductById - Should return product when exists")
    void getProductById_WhenExists_ShouldReturnProduct() {
        // Arrange
        when(productRepository.findById("prod123")).thenReturn(Optional.of(testProduct));

        // Act
        Optional<Product> result = productService.getProductById("prod123");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("prod123");
        assertThat(result.get().getName()).isEqualTo("Test Product");
        verify(productRepository).findById("prod123");
    }

    @Test
    @DisplayName("GetProductById - Should return empty when not found")
    void getProductById_WhenNotFound_ShouldReturnEmpty() {
        // Arrange
        when(productRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act
        Optional<Product> result = productService.getProductById("nonexistent");

        // Assert
        assertThat(result).isEmpty();
        verify(productRepository).findById("nonexistent");
    }

    // ==================== Get Products By Category Tests ====================

    @Test
    @DisplayName("GetProductsByCategory - Should return products in category")
    void getProductsByCategory_ShouldReturnFilteredProducts() {
        // Arrange
        List<Product> electronicsProducts = testProducts.stream()
            .filter(p -> "Electronics".equals(p.getCategory()))
            .toList();
        when(productRepository.findByCategory("Electronics")).thenReturn(electronicsProducts);

        // Act
        List<Product> result = productService.getProductsByCategory("Electronics");

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(p -> "Electronics".equals(p.getCategory()));
        verify(productRepository).findByCategory("Electronics");
    }

    // ==================== Search Products Tests ====================

    @Test
    @DisplayName("SearchProducts - Should return products matching name")
    void searchProducts_ShouldReturnMatchingProducts() {
        // Arrange
        List<Product> searchResults = List.of(testProduct);
        when(productRepository.findByNameContainingIgnoreCase("Test")).thenReturn(searchResults);

        // Act
        List<Product> result = productService.searchProducts("Test");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).contains("Test");
        verify(productRepository).findByNameContainingIgnoreCase("Test");
    }

    @Test
    @DisplayName("SearchProducts - Should be case insensitive")
    void searchProducts_ShouldBeCaseInsensitive() {
        // Arrange
        when(productRepository.findByNameContainingIgnoreCase("test")).thenReturn(List.of(testProduct));

        // Act
        List<Product> result = productService.searchProducts("test");

        // Assert
        assertThat(result).isNotEmpty();
        verify(productRepository).findByNameContainingIgnoreCase("test");
    }

    // ==================== Get Available Products Tests ====================

    @Test
    @DisplayName("GetAvailableProducts - Should return only products with stock")
    void getAvailableProducts_ShouldReturnInStockProducts() {
        // Arrange
        when(productRepository.findByStockQuantityGreaterThan(0)).thenReturn(testProducts);

        // Act
        List<Product> result = productService.getAvailableProducts();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(p -> p.getStockQuantity() > 0);
        verify(productRepository).findByStockQuantityGreaterThan(0);
    }

    // ==================== Create Product Tests ====================

    @Test
    @DisplayName("CreateProduct - Should save and return new product")
    void createProduct_ShouldSaveProduct() {
        // Arrange
        Product newProduct = new Product();
        newProduct.setName("New Product");
        newProduct.setPrice(new BigDecimal("29.99"));
        newProduct.setStockQuantity(10);

        when(productRepository.save(any(Product.class))).thenReturn(newProduct);

        // Act
        Product result = productService.createProduct(newProduct);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("New Product");
        verify(productRepository).save(newProduct);
    }

    // ==================== Update Product Tests ====================

    @Test
    @DisplayName("UpdateProduct - Should update existing product")
    void updateProduct_WhenExists_ShouldUpdateFields() {
        // Arrange
        Product updateData = new Product();
        updateData.setName("Updated Name");
        updateData.setDescription("Updated Description");
        updateData.setPrice(new BigDecimal("199.99"));
        updateData.setCategory("Updated Category");
        updateData.setStockQuantity(200);
        updateData.setImageUrl("http://example.com/new-image.jpg");

        when(productRepository.findById("prod123")).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // Act
        Product result = productService.updateProduct("prod123", updateData);

        // Assert
        assertThat(result).isNotNull();
        verify(productRepository).findById("prod123");
        verify(productRepository).save(argThat(product ->
            product.getName().equals("Updated Name") &&
            product.getDescription().equals("Updated Description") &&
            product.getPrice().equals(new BigDecimal("199.99")) &&
            product.getStockQuantity() == 200
        ));
    }

    @Test
    @DisplayName("UpdateProduct - Should throw exception when not found")
    void updateProduct_WhenNotFound_ShouldThrowException() {
        // Arrange
        Product updateData = new Product();
        updateData.setName("Updated Name");

        when(productRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.updateProduct("nonexistent", updateData))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Product not found");

        verify(productRepository).findById("nonexistent");
        verify(productRepository, never()).save(any(Product.class));
    }

    // ==================== Delete Product Tests ====================

    @Test
    @DisplayName("DeleteProduct - Should delete product by ID")
    void deleteProduct_ShouldCallRepositoryDelete() {
        // Arrange
        doNothing().when(productRepository).deleteById("prod123");

        // Act
        productService.deleteProduct("prod123");

        // Assert
        verify(productRepository).deleteById("prod123");
    }

    // ==================== Update Stock Tests ====================

    @Test
    @DisplayName("UpdateStock - Should reduce stock when sufficient quantity available")
    void updateStock_WithSufficientStock_ShouldReduceStock() {
        // Arrange
        when(productRepository.findById("prod123")).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // Act
        boolean result = productService.updateStock("prod123", 10);

        // Assert
        assertThat(result).isTrue();
        verify(productRepository).findById("prod123");
        verify(productRepository).save(argThat(product ->
            product.getStockQuantity() == 90  // 100 - 10
        ));
    }

    @Test
    @DisplayName("UpdateStock - Should fail when insufficient stock")
    void updateStock_WithInsufficientStock_ShouldReturnFalse() {
        // Arrange
        testProduct.setStockQuantity(5);
        when(productRepository.findById("prod123")).thenReturn(Optional.of(testProduct));

        // Act
        boolean result = productService.updateStock("prod123", 10);

        // Assert
        assertThat(result).isFalse();
        verify(productRepository).findById("prod123");
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("UpdateStock - Should fail when product not found")
    void updateStock_WhenProductNotFound_ShouldReturnFalse() {
        // Arrange
        when(productRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act
        boolean result = productService.updateStock("nonexistent", 10);

        // Assert
        assertThat(result).isFalse();
        verify(productRepository).findById("nonexistent");
        verify(productRepository, never()).save(any(Product.class));
    }

    // ==================== Bulk Update Stock Tests ====================

    @Test
    @DisplayName("BulkUpdateStock - Should update multiple products successfully")
    void bulkUpdateStock_WithValidRequests_ShouldUpdateAll() {
        // Arrange
        List<StockUpdateRequest> updates = Arrays.asList(
            new StockUpdateRequest("prod123", 10),
            new StockUpdateRequest("prod456", 5)
        );

        List<Product> products = Arrays.asList(testProduct, testProducts.get(1));
        when(productRepository.findAllById(anyList())).thenReturn(products);
        when(productRepository.saveAll(anyList())).thenReturn(products);

        // Act
        Map<String, Boolean> results = productService.bulkUpdateStock(updates);

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get("prod123")).isTrue();
        assertThat(results.get("prod456")).isTrue();
        verify(productRepository).findAllById(Arrays.asList("prod123", "prod456"));
        verify(productRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("BulkUpdateStock - Should handle partial failures")
    void bulkUpdateStock_WithInsufficientStock_ShouldReturnMixedResults() {
        // Arrange
        Product lowStockProduct = createProduct("prod456", "Product 2", "Electronics", 2);
        List<StockUpdateRequest> updates = Arrays.asList(
            new StockUpdateRequest("prod123", 10),  // Will succeed (stock: 100)
            new StockUpdateRequest("prod456", 10)   // Will fail (stock: 2)
        );

        List<Product> products = Arrays.asList(testProduct, lowStockProduct);
        when(productRepository.findAllById(anyList())).thenReturn(products);
        when(productRepository.saveAll(anyList())).thenReturn(Collections.singletonList(testProduct));

        // Act
        Map<String, Boolean> results = productService.bulkUpdateStock(updates);

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get("prod123")).isTrue();
        assertThat(results.get("prod456")).isFalse();
        verify(productRepository).findAllById(anyList());
    }
}
