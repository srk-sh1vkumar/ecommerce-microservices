package com.ecommerce.product.service;

import com.ecommerce.common.constants.ErrorCodes;
import com.ecommerce.common.exception.ServiceException;
import com.ecommerce.common.metrics.MetricsService;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProductServiceRefactored.
 * Tests refactored product service with enhanced resilience and validation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceRefactored Tests")
class ProductServiceRefactoredTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private ProductServiceRefactored productService;

    private Product product1;
    private Product product2;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        product1 = new Product(
            "Laptop",
            "High-performance laptop",
            new BigDecimal("999.99"),
            "Electronics",
            50,
            "https://example.com/laptop.jpg"
        );
        product1.setId("prod1");

        product2 = new Product(
            "Mouse",
            "Wireless mouse",
            new BigDecimal("29.99"),
            "Electronics",
            100,
            "https://example.com/mouse.jpg"
        );
        product2.setId("prod2");

        pageable = PageRequest.of(0, 10);
    }

    @Test
    @DisplayName("getAllProducts should return paginated products")
    void getAllProducts_ShouldReturnPaginatedProducts() {
        // Arrange
        Page<Product> productPage = new PageImpl<>(Arrays.asList(product1, product2), pageable, 2);
        when(productRepository.findAll(pageable)).thenReturn(productPage);

        // Act
        Page<Product> result = productService.getAllProducts(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).containsExactly(product1, product2);
        verify(productRepository).findAll(pageable);
    }

    @Test
    @DisplayName("getProductById should return product when found")
    void getProductById_WithValidId_ShouldReturnProduct() {
        // Arrange
        when(productRepository.findById("prod1")).thenReturn(Optional.of(product1));
        doNothing().when(metricsService).incrementProductViews();

        // Act
        Product result = productService.getProductById("prod1");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("prod1");
        assertThat(result.getName()).isEqualTo("Laptop");
        verify(productRepository).findById("prod1");
        verify(metricsService).incrementProductViews();
    }

    @Test
    @DisplayName("getProductById should throw exception when product not found")
    void getProductById_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(productRepository.findById("invalid")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.getProductById("invalid"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("Product not found");

        verify(productRepository).findById("invalid");
        verify(metricsService, never()).incrementProductViews();
    }

    @Test
    @DisplayName("getProductById should throw exception for blank ID")
    void getProductById_WithBlankId_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> productService.getProductById(""))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("Product ID is required");

        verify(productRepository, never()).findById(anyString());
    }

    @Test
    @DisplayName("getProductsByCategory should return products in category")
    void getProductsByCategory_ShouldReturnProducts() {
        // Arrange
        Page<Product> productPage = new PageImpl<>(Arrays.asList(product1, product2), pageable, 2);
        when(productRepository.findByCategory("Electronics", pageable)).thenReturn(productPage);

        // Act
        Page<Product> result = productService.getProductsByCategory("Electronics", pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        verify(productRepository).findByCategory("Electronics", pageable);
    }

    @Test
    @DisplayName("getProductsByCategory should throw exception for blank category")
    void getProductsByCategory_WithBlankCategory_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> productService.getProductsByCategory("", pageable))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("Category is required");

        verify(productRepository, never()).findByCategory(anyString(), any());
    }

    @Test
    @DisplayName("searchProducts should return matching products")
    void searchProducts_ShouldReturnMatchingProducts() {
        // Arrange
        when(productRepository.findByNameContainingIgnoreCase("Laptop"))
            .thenReturn(Arrays.asList(product1));
        doNothing().when(metricsService).incrementProductSearches();

        // Act
        List<Product> result = productService.searchProducts("Laptop");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Laptop");
        verify(productRepository).findByNameContainingIgnoreCase("Laptop");
        verify(metricsService).incrementProductSearches();
    }

    @Test
    @DisplayName("searchProducts should throw exception for blank search term")
    void searchProducts_WithBlankName_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> productService.searchProducts(""))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("Search term is required");

        verify(productRepository, never()).findByNameContainingIgnoreCase(anyString());
    }

    @Test
    @DisplayName("getAvailableProducts should return products with stock")
    void getAvailableProducts_ShouldReturnProductsWithStock() {
        // Arrange
        when(productRepository.findByStockQuantityGreaterThan(0))
            .thenReturn(Arrays.asList(product1, product2));

        // Act
        List<Product> result = productService.getAvailableProducts();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(product1, product2);
        verify(productRepository).findByStockQuantityGreaterThan(0);
    }

    @Test
    @DisplayName("createProduct should create and return product")
    void createProduct_WithValidProduct_ShouldSucceed() {
        // Arrange
        Product newProduct = new Product(
            "Keyboard",
            "Mechanical keyboard",
            new BigDecimal("79.99"),
            "Electronics",
            30,
            "https://example.com/keyboard.jpg"
        );
        Product savedProduct = new Product(
            "Keyboard",
            "Mechanical keyboard",
            new BigDecimal("79.99"),
            "Electronics",
            30,
            "https://example.com/keyboard.jpg"
        );
        savedProduct.setId("prod3");

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        doNothing().when(metricsService).incrementProductCreations();

        // Act
        Product result = productService.createProduct(newProduct);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("prod3");
        assertThat(result.getName()).isEqualTo("Keyboard");
        verify(productRepository).save(any(Product.class));
        verify(metricsService).incrementProductCreations();
    }

    @Test
    @DisplayName("createProduct should throw exception for blank name")
    void createProduct_WithBlankName_ShouldThrowException() {
        // Arrange
        Product invalidProduct = new Product(
            "",
            "Description",
            new BigDecimal("99.99"),
            "Electronics",
            10,
            "https://example.com/image.jpg"
        );

        // Act & Assert
        assertThatThrownBy(() -> productService.createProduct(invalidProduct))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("Product name is required");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("createProduct should throw exception for blank category")
    void createProduct_WithBlankCategory_ShouldThrowException() {
        // Arrange
        Product invalidProduct = new Product(
            "Product",
            "Description",
            new BigDecimal("99.99"),
            "",
            10,
            "https://example.com/image.jpg"
        );

        // Act & Assert
        assertThatThrownBy(() -> productService.createProduct(invalidProduct))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("Product category is required");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("createProduct should throw exception for negative price")
    void createProduct_WithNegativePrice_ShouldThrowException() {
        // Arrange
        Product invalidProduct = new Product(
            "Product",
            "Description",
            new BigDecimal("-10.00"),
            "Electronics",
            10,
            "https://example.com/image.jpg"
        );

        // Act & Assert
        assertThatThrownBy(() -> productService.createProduct(invalidProduct))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("Price must be greater than zero");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("createProduct should throw exception for negative stock")
    void createProduct_WithNegativeStock_ShouldThrowException() {
        // Arrange
        Product invalidProduct = new Product(
            "Product",
            "Description",
            new BigDecimal("99.99"),
            "Electronics",
            -5,
            "https://example.com/image.jpg"
        );

        // Act & Assert
        assertThatThrownBy(() -> productService.createProduct(invalidProduct))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("Stock quantity must be a positive number");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateProduct should update and return product")
    void updateProduct_WithValidData_ShouldSucceed() {
        // Arrange
        Product updatedDetails = new Product(
            "Updated Laptop",
            "Updated description",
            new BigDecimal("1099.99"),
            "Electronics",
            40,
            "https://example.com/updated.jpg"
        );

        when(productRepository.findById("prod1")).thenReturn(Optional.of(product1));
        when(productRepository.save(any(Product.class))).thenReturn(product1);

        // Act
        Product result = productService.updateProduct("prod1", updatedDetails);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Laptop");
        verify(productRepository).findById("prod1");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("updateProduct should throw exception when product not found")
    void updateProduct_WithInvalidId_ShouldThrowException() {
        // Arrange
        Product updatedDetails = new Product(
            "Updated Product",
            "Description",
            new BigDecimal("99.99"),
            "Electronics",
            10,
            "https://example.com/image.jpg"
        );

        when(productRepository.findById("invalid")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.updateProduct("invalid", updatedDetails))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("Product not found");

        verify(productRepository).findById("invalid");
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateProduct should throw exception for blank ID")
    void updateProduct_WithBlankId_ShouldThrowException() {
        // Arrange
        Product updatedDetails = new Product(
            "Product",
            "Description",
            new BigDecimal("99.99"),
            "Electronics",
            10,
            "https://example.com/image.jpg"
        );

        // Act & Assert
        assertThatThrownBy(() -> productService.updateProduct("", updatedDetails))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("Product ID is required");

        verify(productRepository, never()).findById(anyString());
    }

    @Test
    @DisplayName("deleteProduct should delete product when it exists")
    void deleteProduct_WithValidId_ShouldSucceed() {
        // Arrange
        when(productRepository.existsById("prod1")).thenReturn(true);
        doNothing().when(productRepository).deleteById("prod1");

        // Act
        productService.deleteProduct("prod1");

        // Assert
        verify(productRepository).existsById("prod1");
        verify(productRepository).deleteById("prod1");
    }

    @Test
    @DisplayName("deleteProduct should throw exception when product not found")
    void deleteProduct_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(productRepository.existsById("invalid")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> productService.deleteProduct("invalid"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("Product not found");

        verify(productRepository).existsById("invalid");
        verify(productRepository, never()).deleteById(anyString());
    }

    @Test
    @DisplayName("deleteProduct should throw exception for blank ID")
    void deleteProduct_WithBlankId_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> productService.deleteProduct(""))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("Product ID is required");

        verify(productRepository, never()).existsById(anyString());
    }

    @Test
    @DisplayName("updateStock should decrease stock quantity")
    void updateStock_WithValidData_ShouldSucceed() {
        // Arrange
        product1.setStockQuantity(50);
        when(productRepository.findById("prod1")).thenReturn(Optional.of(product1));
        when(productRepository.save(any(Product.class))).thenReturn(product1);

        // Act
        boolean result = productService.updateStock("prod1", 10);

        // Assert
        assertThat(result).isTrue();
        assertThat(product1.getStockQuantity()).isEqualTo(40);
        verify(productRepository).findById("prod1");
        verify(productRepository).save(product1);
    }

    @Test
    @DisplayName("updateStock should throw exception when product not found")
    void updateStock_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(productRepository.findById("invalid")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.updateStock("invalid", 10))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("Product not found");

        verify(productRepository).findById("invalid");
    }

    @Test
    @DisplayName("updateStock should throw exception for insufficient stock")
    void updateStock_WithInsufficientStock_ShouldThrowException() {
        // Arrange
        product1.setStockQuantity(5);
        when(productRepository.findById("prod1")).thenReturn(Optional.of(product1));

        // Act & Assert
        assertThatThrownBy(() -> productService.updateStock("prod1", 10))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("Insufficient stock");

        verify(productRepository).findById("prod1");
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStock should throw exception for blank ID")
    void updateStock_WithBlankId_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> productService.updateStock("", 10))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("Product ID is required");

        verify(productRepository, never()).findById(anyString());
    }

    @Test
    @DisplayName("updateStock should throw exception for non-positive quantity")
    void updateStock_WithZeroQuantity_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> productService.updateStock("prod1", 0))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("Quantity must be a positive number");

        verify(productRepository, never()).findById(anyString());
    }

    @Test
    @DisplayName("existsById should return true when product exists")
    void existsById_WithExistingId_ShouldReturnTrue() {
        // Arrange
        when(productRepository.existsById("prod1")).thenReturn(true);

        // Act
        boolean result = productService.existsById("prod1");

        // Assert
        assertThat(result).isTrue();
        verify(productRepository).existsById("prod1");
    }

    @Test
    @DisplayName("existsById should return false when product does not exist")
    void existsById_WithNonExistingId_ShouldReturnFalse() {
        // Arrange
        when(productRepository.existsById("invalid")).thenReturn(false);

        // Act
        boolean result = productService.existsById("invalid");

        // Assert
        assertThat(result).isFalse();
        verify(productRepository).existsById("invalid");
    }

    @Test
    @DisplayName("existsById should throw exception for blank ID")
    void existsById_WithBlankId_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> productService.existsById(""))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("Product ID is required");

        verify(productRepository, never()).existsById(anyString());
    }
}
