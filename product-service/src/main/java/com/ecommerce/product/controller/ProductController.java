package com.ecommerce.product.controller;

import com.ecommerce.product.entity.Product;
import com.ecommerce.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Product Controller with comprehensive API documentation.
 * Manages product catalog operations including CRUD and search.
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Product Management", description = "APIs for managing products, search, and inventory")
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    @Operation(
            summary = "Get All Products",
            description = "Retrieves paginated list of all products in the catalog"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Page.class)))
    })
    @GetMapping
    public ResponseEntity<Page<Product>> getAllProducts(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productService.getAllProducts(pageable);
        return ResponseEntity.ok(products);
    }
    
    @Operation(
            summary = "Get Product by ID",
            description = "Retrieves detailed information about a specific product"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product found",
                    content = @Content(schema = @Schema(implementation = Product.class))),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(
            @Parameter(description = "Product ID", required = true, example = "507f1f77bcf86cd799439011")
            @PathVariable String id) {
        Optional<Product> product = productService.getProductById(id);
        return product.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/category/{category}")
    public ResponseEntity<Page<Product>> getProductsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Product> products = productService.getProductsByCategoryPaged(category, pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Product>> searchProducts(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Product> products = productService.searchProductsPaged(name, pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/available")
    public ResponseEntity<Page<Product>> getAvailableProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Product> products = productService.getAvailableProductsPaged(pageable);
        return ResponseEntity.ok(products);
    }
    
    @Operation(
            summary = "Create Product",
            description = "Creates a new product in the catalog. Requires ADMIN or MANAGER role.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product created successfully",
                    content = @Content(schema = @Schema(implementation = Product.class))),
            @ApiResponse(responseCode = "400", description = "Invalid product data"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Product> createProduct(
            @Parameter(description = "Product data", required = true)
            @jakarta.validation.Valid @RequestBody Product product) {
        Product savedProduct = productService.createProduct(product);
        return ResponseEntity.ok(savedProduct);
    }

    @Operation(
            summary = "Update Product",
            description = "Updates an existing product. Requires ADMIN or MANAGER role.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product updated successfully",
                    content = @Content(schema = @Schema(implementation = Product.class))),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Product> updateProduct(
            @Parameter(description = "Product ID", required = true)
            @PathVariable String id,
            @Parameter(description = "Updated product data", required = true)
            @jakarta.validation.Valid @RequestBody Product product) {
        try {
            Product updatedProduct = productService.updateProduct(id, product);
            return ResponseEntity.ok(updatedProduct);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @Operation(
            summary = "Delete Product",
            description = "Permanently deletes a product from catalog. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions - ADMIN role required")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product ID to delete", required = true)
            @PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{id}/stock")
    public ResponseEntity<Boolean> updateStock(@PathVariable String id, @RequestParam Integer quantity) {
        boolean updated = productService.updateStock(id, quantity);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/stock/bulk")
    public ResponseEntity<com.ecommerce.product.dto.BulkStockUpdateResponse> bulkUpdateStock(
            @jakarta.validation.Valid @RequestBody java.util.List<com.ecommerce.product.dto.StockUpdateRequest> updates) {
        java.util.Map<String, Boolean> results = productService.bulkUpdateStock(updates);
        com.ecommerce.product.dto.BulkStockUpdateResponse response =
                new com.ecommerce.product.dto.BulkStockUpdateResponse(results);
        return ResponseEntity.ok(response);
    }
}