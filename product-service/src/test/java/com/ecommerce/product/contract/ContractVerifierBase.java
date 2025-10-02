package com.ecommerce.product.contract;

import com.ecommerce.product.controller.ProductController;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.service.ProductService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Base class for Spring Cloud Contract verification tests.
 * Sets up the test context and provides mock data for contract validation.
 */
@WebMvcTest(ProductController.class)
public abstract class ContractVerifierBase {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @BeforeEach
    public void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);

        // Setup mock data for contracts

        // Mock getProductById
        Product testProduct = createTestProduct(
                "test-product-123",
                "Test Wireless Mouse",
                "A high-quality wireless mouse",
                new BigDecimal("29.99"),
                "Electronics",
                150,
                "https://example.com/images/mouse.jpg"
        );
        when(productService.getProductById("test-product-123"))
                .thenReturn(Optional.of(testProduct));

        // Mock getProductsByCategory
        Product product1 = createTestProduct(
                "prod-001",
                "Wireless Keyboard",
                "Ergonomic wireless keyboard",
                new BigDecimal("49.99"),
                "Electronics",
                75,
                "https://example.com/images/keyboard.jpg"
        );
        Product product2 = createTestProduct(
                "prod-002",
                "USB Mouse",
                "Optical USB mouse",
                new BigDecimal("19.99"),
                "Electronics",
                120,
                "https://example.com/images/mouse.jpg"
        );
        when(productService.getProductsByCategory(eq("Electronics"), any()))
                .thenReturn(Arrays.asList(product1, product2));

        // Mock updateStock
        when(productService.updateStock("test-product-123", 5))
                .thenReturn(true);
    }

    private Product createTestProduct(String id, String name, String description,
                                       BigDecimal price, String category,
                                       Integer stockQuantity, String imageUrl) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setCategory(category);
        product.setStockQuantity(stockQuantity);
        product.setImageUrl(imageUrl);
        return product;
    }
}
