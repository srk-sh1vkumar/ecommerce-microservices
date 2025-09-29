package com.ecommerce.product.service;

import com.ecommerce.common.constants.ErrorCodes;
import com.ecommerce.common.exception.ServiceException;
import com.ecommerce.common.util.ValidationUtils;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;

/**
 * Refactored Product Service with enhanced resilience and validation.
 * Uses common library utilities for consistent behavior across services.
 *
 * Improvements:
 * - Integrated common exception handling
 * - Enhanced validation using shared utilities
 * - Declarative circuit breaker and retry annotations
 * - Better caching strategy with eviction
 * - Consistent error codes
 * - Improved logging
 *
 * @author E-commerce Development Team
 * @version 2.0
 */
@Service
@Transactional
public class ProductServiceRefactored {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceRefactored.class);
    private static final String CIRCUIT_BREAKER_NAME = "productService";

    private final ProductRepository productRepository;

    @Autowired
    public ProductServiceRefactored(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Retrieves all products with pagination and caching.
     *
     * @param pageable Pagination parameters
     * @return Page of products
     */
    @Cacheable(value = "products", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getAllProductsFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    @Transactional(readOnly = true)
    public Page<Product> getAllProducts(Pageable pageable) {
        logger.debug("Fetching products - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        return productRepository.findAll(pageable);
    }

    /**
     * Fallback method for getAllProducts.
     */
    private Page<Product> getAllProductsFallback(Pageable pageable, Exception e) {
        logger.error("Failed to fetch products, returning empty page", e);
        return Page.empty();
    }

    /**
     * Retrieves a product by ID with caching.
     *
     * @param id Product ID
     * @return Product
     * @throws ServiceException if product not found
     */
    @Cacheable(value = "product", key = "#id")
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getProductByIdFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    @Transactional(readOnly = true)
    public Product getProductById(String id) {
        logger.debug("Fetching product by ID: {}", id);

        ValidationUtils.validateId(id, "Product");

        return productRepository.findById(id)
                .orElseThrow(() -> ServiceException.notFound(
                        "Product not found with ID: " + id,
                        ErrorCodes.PRODUCT_NOT_FOUND
                ));
    }

    /**
     * Fallback method for getProductById.
     */
    private Product getProductByIdFallback(String id, Exception e) {
        logger.error("Failed to fetch product by ID: {}", id, e);
        throw ServiceException.serviceUnavailable("Product service temporarily unavailable");
    }

    /**
     * Retrieves products by category.
     *
     * @param category Product category
     * @param pageable Pagination parameters
     * @return Page of products in category
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getProductsByCategoryFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    @Transactional(readOnly = true)
    public Page<Product> getProductsByCategory(String category, Pageable pageable) {
        logger.debug("Fetching products by category: {}", category);

        ValidationUtils.validateNotBlank(category, "Category");

        return productRepository.findByCategory(category, pageable);
    }

    /**
     * Fallback for getProductsByCategory.
     */
    private Page<Product> getProductsByCategoryFallback(String category, Pageable pageable, Exception e) {
        logger.error("Failed to fetch products by category: {}", category, e);
        return Page.empty();
    }

    /**
     * Searches products by name.
     *
     * @param name Search term
     * @return List of matching products
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "searchProductsFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    @Transactional(readOnly = true)
    public List<Product> searchProducts(String name) {
        logger.debug("Searching products with name: {}", name);

        ValidationUtils.validateNotBlank(name, "Search term");

        return productRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * Fallback for searchProducts.
     */
    private List<Product> searchProductsFallback(String name, Exception e) {
        logger.error("Failed to search products", e);
        return Collections.emptyList();
    }

    /**
     * Retrieves products with available stock.
     *
     * @return List of available products
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getAvailableProductsFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    @Transactional(readOnly = true)
    public List<Product> getAvailableProducts() {
        logger.debug("Fetching available products");
        return productRepository.findByStockQuantityGreaterThan(0);
    }

    /**
     * Fallback for getAvailableProducts.
     */
    private List<Product> getAvailableProductsFallback(Exception e) {
        logger.error("Failed to fetch available products", e);
        return Collections.emptyList();
    }

    /**
     * Creates a new product.
     *
     * @param product Product to create
     * @return Created product
     * @throws ServiceException if validation fails
     */
    @CacheEvict(value = "products", allEntries = true)
    public Product createProduct(@Valid Product product) {
        logger.info("Creating new product: {}", product.getName());

        // Validate product data
        validateProduct(product);

        Product savedProduct = productRepository.save(product);
        logger.info("Product created successfully with ID: {}", savedProduct.getId());

        return savedProduct;
    }

    /**
     * Updates an existing product.
     *
     * @param id Product ID
     * @param productDetails Updated product data
     * @return Updated product
     * @throws ServiceException if product not found or validation fails
     */
    @CacheEvict(value = {"product", "products"}, key = "#id", allEntries = true)
    public Product updateProduct(String id, @Valid Product productDetails) {
        logger.info("Updating product with ID: {}", id);

        ValidationUtils.validateId(id, "Product");
        validateProduct(productDetails);

        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> ServiceException.notFound(
                        "Product not found with ID: " + id,
                        ErrorCodes.PRODUCT_NOT_FOUND
                ));

        // Update fields
        existingProduct.setName(productDetails.getName());
        existingProduct.setDescription(productDetails.getDescription());
        existingProduct.setPrice(productDetails.getPrice());
        existingProduct.setCategory(productDetails.getCategory());
        existingProduct.setStockQuantity(productDetails.getStockQuantity());
        existingProduct.setImageUrl(productDetails.getImageUrl());

        Product updatedProduct = productRepository.save(existingProduct);
        logger.info("Product updated successfully: {}", id);

        return updatedProduct;
    }

    /**
     * Deletes a product.
     *
     * @param id Product ID to delete
     * @throws ServiceException if product not found
     */
    @CacheEvict(value = {"product", "products"}, key = "#id", allEntries = true)
    public void deleteProduct(String id) {
        logger.info("Deleting product with ID: {}", id);

        ValidationUtils.validateId(id, "Product");

        if (!productRepository.existsById(id)) {
            throw ServiceException.notFound(
                    "Product not found with ID: " + id,
                    ErrorCodes.PRODUCT_NOT_FOUND
            );
        }

        productRepository.deleteById(id);
        logger.info("Product deleted successfully: {}", id);
    }

    /**
     * Updates product stock quantity.
     *
     * @param productId Product ID
     * @param quantity Quantity to deduct
     * @return true if stock updated successfully
     * @throws ServiceException if insufficient stock or product not found
     */
    @CacheEvict(value = "product", key = "#productId")
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "updateStockFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public boolean updateStock(String productId, Integer quantity) {
        logger.info("Updating stock for product: {}, quantity: {}", productId, quantity);

        ValidationUtils.validateId(productId, "Product");
        ValidationUtils.validatePositive(quantity, "Quantity");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ServiceException.notFound(
                        "Product not found with ID: " + productId,
                        ErrorCodes.PRODUCT_NOT_FOUND
                ));

        // Validate stock availability
        ValidationUtils.validateStock(quantity, product.getStockQuantity());

        // Update stock
        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);

        logger.info("Stock updated successfully for product: {}", productId);
        return true;
    }

    /**
     * Fallback for updateStock.
     */
    private boolean updateStockFallback(String productId, Integer quantity, Exception e) {
        logger.error("Failed to update stock for product: {}", productId, e);
        throw ServiceException.internalError("Failed to update product stock", e);
    }

    /**
     * Validates product data.
     *
     * @param product Product to validate
     * @throws ServiceException if validation fails
     */
    private void validateProduct(Product product) {
        ValidationUtils.validateNotBlank(product.getName(), "Product name");
        ValidationUtils.validateNotBlank(product.getCategory(), "Product category");
        ValidationUtils.validatePrice(product.getPrice());
        ValidationUtils.validatePositive(product.getStockQuantity(), "Stock quantity");
    }

    /**
     * Checks if a product exists.
     *
     * @param id Product ID
     * @return true if product exists
     */
    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        ValidationUtils.validateId(id, "Product");
        return productRepository.existsById(id);
    }
}