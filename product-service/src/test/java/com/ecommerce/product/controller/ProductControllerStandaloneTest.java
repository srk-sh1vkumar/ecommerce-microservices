package com.ecommerce.product.controller;

import com.ecommerce.product.entity.Product;
import com.ecommerce.product.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Standalone unit tests for ProductController using MockMvc without Spring context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductController Standalone Tests")
class ProductControllerStandaloneTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private Product testProduct;
    private List<Product> productList;
    private Page<Product> productPage;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        testProduct = new Product();
        testProduct.setId("prod123");
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setCategory("Electronics");
        testProduct.setStockQuantity(50);
        testProduct.setImageUrl("https://example.com/product.jpg");

        Product product2 = new Product();
        product2.setId("prod456");
        product2.setName("Product 2");
        product2.setPrice(new BigDecimal("49.99"));
        product2.setCategory("Electronics");
        product2.setStockQuantity(30);

        productList = Arrays.asList(testProduct, product2);
        productPage = new PageImpl<>(productList, PageRequest.of(0, 10), productList.size());
    }

    @Test
    @DisplayName("Get All Products - Should return 200 with paginated products")
    void getAllProducts_ShouldReturn200() throws Exception {
        // Arrange
        when(productService.getAllProducts(any())).thenReturn(productPage);

        // Act & Assert
        mockMvc.perform(get("/api/products")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].name").value("Test Product"))
            .andExpect(jsonPath("$.totalElements").value(2));

        verify(productService).getAllProducts(any());
    }

    @Test
    @DisplayName("Get Product By ID - Should return 200 with product")
    void getProductById_WithValidId_ShouldReturn200() throws Exception {
        // Arrange
        when(productService.getProductById("prod123")).thenReturn(Optional.of(testProduct));

        // Act & Assert
        mockMvc.perform(get("/api/products/prod123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("prod123"))
            .andExpect(jsonPath("$.name").value("Test Product"))
            .andExpect(jsonPath("$.price").value(99.99))
            .andExpect(jsonPath("$.stockQuantity").value(50));

        verify(productService).getProductById("prod123");
    }

    @Test
    @DisplayName("Get Product By ID - Should return 404 when not found")
    void getProductById_WhenNotFound_ShouldReturn404() throws Exception {
        // Arrange
        when(productService.getProductById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/products/nonexistent"))
            .andExpect(status().isNotFound());

        verify(productService).getProductById("nonexistent");
    }

    @Test
    @DisplayName("Get Products By Category - Should return 200 with products")
    void getProductsByCategory_ShouldReturn200() throws Exception {
        // Arrange
        when(productService.getProductsByCategoryPaged(eq("Electronics"), any())).thenReturn(productPage);

        // Act & Assert
        mockMvc.perform(get("/api/products/category/Electronics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].category").value("Electronics"));

        verify(productService).getProductsByCategoryPaged(eq("Electronics"), any());
    }

    @Test
    @DisplayName("Create Product - Should return 200 with created product")
    void createProduct_WithValidData_ShouldReturn200() throws Exception {
        // Arrange
        when(productService.createProduct(any(Product.class))).thenReturn(testProduct);

        // Act & Assert
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testProduct)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("prod123"))
            .andExpect(jsonPath("$.name").value("Test Product"));

        verify(productService).createProduct(any(Product.class));
    }

    @Test
    @DisplayName("Update Product - Should return 200 with updated product")
    void updateProduct_WithValidData_ShouldReturn200() throws Exception {
        // Arrange
        when(productService.updateProduct(eq("prod123"), any(Product.class)))
            .thenReturn(testProduct);

        // Act & Assert
        mockMvc.perform(put("/api/products/prod123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testProduct)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("prod123"));

        verify(productService).updateProduct(eq("prod123"), any(Product.class));
    }

    @Test
    @DisplayName("Update Product - Should return 404 when product not found")
    void updateProduct_WhenNotFound_ShouldReturn404() throws Exception {
        // Arrange
        when(productService.updateProduct(eq("nonexistent"), any(Product.class)))
            .thenThrow(new RuntimeException("Product not found"));

        // Act & Assert
        mockMvc.perform(put("/api/products/nonexistent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testProduct)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Delete Product - Should return 200")
    void deleteProduct_WithValidId_ShouldReturn200() throws Exception {
        // Arrange
        doNothing().when(productService).deleteProduct("prod123");

        // Act & Assert
        mockMvc.perform(delete("/api/products/prod123"))
            .andExpect(status().isOk());

        verify(productService).deleteProduct("prod123");
    }

    @Test
    @DisplayName("Update Stock - Should return 200")
    void updateStock_WithValidData_ShouldReturn200() throws Exception {
        // Arrange
        when(productService.updateStock("prod123", 10)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(put("/api/products/prod123/stock")
                .param("quantity", "10"))
            .andExpect(status().isOk())
            .andExpect(content().string("true"));

        verify(productService).updateStock("prod123", 10);
    }

    @Test
    @DisplayName("Search Products - Should return 200 with results")
    void searchProducts_ShouldReturn200() throws Exception {
        // Arrange
        when(productService.searchProductsPaged(eq("Test"), any())).thenReturn(productPage);

        // Act & Assert
        mockMvc.perform(get("/api/products/search")
                .param("name", "Test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(2));

        verify(productService).searchProductsPaged(eq("Test"), any());
    }

    @Test
    @DisplayName("Bulk Update Stock - Should return 200 with response")
    void bulkUpdateStock_WithValidData_ShouldReturn200() throws Exception {
        // Arrange
        com.ecommerce.product.dto.StockUpdateRequest request1 =
            new com.ecommerce.product.dto.StockUpdateRequest("prod1", 5);
        com.ecommerce.product.dto.StockUpdateRequest request2 =
            new com.ecommerce.product.dto.StockUpdateRequest("prod2", 10);

        List<com.ecommerce.product.dto.StockUpdateRequest> requests = Arrays.asList(request1, request2);

        java.util.Map<String, Boolean> results = new java.util.HashMap<>();
        results.put("prod1", true);
        results.put("prod2", true);

        when(productService.bulkUpdateStock(anyList())).thenReturn(results);

        // Act & Assert
        mockMvc.perform(put("/api/products/stock/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requests)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.successCount").value(2))
            .andExpect(jsonPath("$.failureCount").value(0));

        verify(productService).bulkUpdateStock(anyList());
    }
}
