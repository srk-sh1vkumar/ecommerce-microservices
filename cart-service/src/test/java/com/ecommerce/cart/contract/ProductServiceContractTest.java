package com.ecommerce.cart.contract;

import com.ecommerce.cart.dto.ProductDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consumer contract test for Cart Service -> Product Service interaction.
 * Uses stubs generated from Product Service contracts to verify integration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner(
        ids = "com.ecommerce:product-service:+:stubs:8082",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
public class ProductServiceContractTest {

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    public void shouldGetProductById() {
        // Given
        String productId = "test-product-123";
        String url = "http://localhost:8082/api/products/" + productId;

        // When
        ResponseEntity<ProductDTO> response = restTemplate.getForEntity(url, ProductDTO.class);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(productId);
        assertThat(response.getBody().getName()).isEqualTo("Test Wireless Mouse");
        assertThat(response.getBody().getPrice()).isEqualTo(new BigDecimal("29.99"));
        assertThat(response.getBody().getCategory()).isEqualTo("Electronics");
        assertThat(response.getBody().getStockQuantity()).isEqualTo(150);
    }

    @Test
    public void shouldGetProductsByCategory() {
        // Given
        String category = "Electronics";
        String url = "http://localhost:8082/api/products/category/" + category;

        // When
        ResponseEntity<ProductDTO[]> response = restTemplate.getForEntity(url, ProductDTO[].class);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().length).isGreaterThan(0);
        assertThat(response.getBody()[0].getCategory()).isEqualTo("Electronics");
    }

    @Test
    public void shouldUpdateProductStock() {
        // Given
        String productId = "test-product-123";
        int quantity = 5;
        String url = "http://localhost:8082/api/products/" + productId + "/stock?quantity=" + quantity;

        // When
        restTemplate.put(url, null);

        // Then - no exception means success
        // The contract defines status 200 for successful stock update
    }
}
