package com.ecommerce.product.service;

import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductRepository productRepository;

    private final CircuitBreaker databaseCircuitBreaker;
    private final Retry databaseRetry;

    public ProductService(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.databaseCircuitBreaker = circuitBreakerRegistry.circuitBreaker("database");

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(2))
                .retryExceptions(Exception.class)
                .build();
        this.databaseRetry = Retry.of("database", retryConfig);
    }
    
    @Cacheable(value = "products", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Product> getAllProducts(Pageable pageable) {
        Supplier<Page<Product>> productSupplier = () -> {
            logger.info("Fetching products from database - page: {}, size: {}",
                       pageable.getPageNumber(), pageable.getPageSize());
            return productRepository.findAll(pageable);
        };

        return executeWithCircuitBreakerAndRetry(productSupplier, "getAllProducts");
    }

    @Cacheable(value = "product", key = "#id")
    public Optional<Product> getProductById(String id) {
        Supplier<Optional<Product>> productSupplier = () -> {
            logger.info("Fetching product by ID: {}", id);
            return productRepository.findById(id);
        };

        return executeWithCircuitBreakerAndRetry(productSupplier, "getProductById");
    }
    
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }
    
    public Page<Product> getProductsByCategoryPaged(String category, Pageable pageable) {
        return productRepository.findByCategory(category, pageable);
    }
    
    public List<Product> searchProducts(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }
    
    public List<Product> getAvailableProducts() {
        return productRepository.findByStockQuantityGreaterThan(0);
    }
    
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }
    
    public Product updateProduct(String id, Product productDetails) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setCategory(productDetails.getCategory());
        product.setStockQuantity(productDetails.getStockQuantity());
        product.setImageUrl(productDetails.getImageUrl());
        
        return productRepository.save(product);
    }
    
    public void deleteProduct(String id) {
        productRepository.deleteById(id);
    }
    
    @CacheEvict(value = "product", key = "#productId")
    public boolean updateStock(String productId, Integer quantity) {
        Supplier<Boolean> updateSupplier = () -> {
            logger.info("Updating stock for product: {}, quantity: {}", productId, quantity);
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                if (product.getStockQuantity() >= quantity) {
                    product.setStockQuantity(product.getStockQuantity() - quantity);
                    productRepository.save(product);
                    return true;
                }
            }
            return false;
        };

        return executeWithCircuitBreakerAndRetry(updateSupplier, "updateStock");
    }

    /**
     * Execute database operations with circuit breaker and retry patterns
     */
    private <T> T executeWithCircuitBreakerAndRetry(Supplier<T> supplier, String operation) {
        Supplier<T> decoratedSupplier = CircuitBreaker
                .decorateSupplier(databaseCircuitBreaker, supplier);

        decoratedSupplier = Retry.decorateSupplier(databaseRetry, decoratedSupplier);

        try {
            T result = decoratedSupplier.get();
            logger.debug("Successfully executed operation: {}", operation);
            return result;
        } catch (Exception e) {
            logger.error("Failed to execute operation: {} - Error: {}", operation, e.getMessage());
            return handleFallback(operation, e);
        }
    }

    /**
     * Fallback method for failed operations
     */
    @SuppressWarnings("unchecked")
    private <T> T handleFallback(String operation, Exception e) {
        logger.warn("Executing fallback for operation: {}", operation);

        switch (operation) {
            case "getAllProducts":
                return (T) Page.empty();
            case "getProductById":
                return (T) Optional.empty();
            case "getProductsByCategory":
            case "getAvailableProducts":
            case "searchProducts":
                return (T) Collections.emptyList();
            case "updateStock":
                return (T) Boolean.FALSE;
            default:
                throw new RuntimeException("Operation failed: " + operation, e);
        }
    }
}