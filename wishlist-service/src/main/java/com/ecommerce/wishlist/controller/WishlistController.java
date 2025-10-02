package com.ecommerce.wishlist.controller;

import com.ecommerce.wishlist.dto.AddToWishlistRequest;
import com.ecommerce.wishlist.entity.Wishlist;
import com.ecommerce.wishlist.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for wishlist operations.
 * Allows users to save products for later purchase.
 */
@RestController
@RequestMapping("/api/wishlist")
@Tag(name = "Wishlist", description = "Wishlist management APIs")
public class WishlistController {

    @Autowired
    private WishlistService wishlistService;

    @Operation(
            summary = "Get user's wishlist",
            description = "Retrieves all items in the user's wishlist",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Wishlist retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Wishlist not found")
            }
    )
    @GetMapping("/{userEmail}")
    public ResponseEntity<Wishlist> getWishlist(
            @Parameter(description = "User's email address", required = true)
            @PathVariable String userEmail) {
        Wishlist wishlist = wishlistService.getWishlist(userEmail);
        return ResponseEntity.ok(wishlist);
    }

    @Operation(
            summary = "Add product to wishlist",
            description = "Adds a product to the user's wishlist",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Product added to wishlist"),
                    @ApiResponse(responseCode = "400", description = "Product already in wishlist"),
                    @ApiResponse(responseCode = "404", description = "Product not found")
            }
    )
    @PostMapping
    public ResponseEntity<Wishlist> addToWishlist(
            @Valid @RequestBody AddToWishlistRequest request) {
        Wishlist wishlist = wishlistService.addToWishlist(
                request.getUserEmail(),
                request.getProductId()
        );
        return ResponseEntity.ok(wishlist);
    }

    @Operation(
            summary = "Remove product from wishlist",
            description = "Removes a specific product from the user's wishlist",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Product removed from wishlist"),
                    @ApiResponse(responseCode = "404", description = "Product or wishlist not found")
            }
    )
    @DeleteMapping("/{userEmail}/items/{productId}")
    public ResponseEntity<Wishlist> removeFromWishlist(
            @Parameter(description = "User's email address", required = true)
            @PathVariable String userEmail,
            @Parameter(description = "Product ID to remove", required = true)
            @PathVariable String productId) {
        Wishlist wishlist = wishlistService.removeFromWishlist(userEmail, productId);
        return ResponseEntity.ok(wishlist);
    }

    @Operation(
            summary = "Clear wishlist",
            description = "Removes all items from the user's wishlist",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Wishlist cleared successfully"),
                    @ApiResponse(responseCode = "404", description = "Wishlist not found")
            }
    )
    @DeleteMapping("/{userEmail}")
    public ResponseEntity<Void> clearWishlist(
            @Parameter(description = "User's email address", required = true)
            @PathVariable String userEmail) {
        wishlistService.clearWishlist(userEmail);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get wishlist item count",
            description = "Returns the number of items in the user's wishlist",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Count retrieved successfully")
            }
    )
    @GetMapping("/{userEmail}/count")
    public ResponseEntity<Map<String, Integer>> getWishlistCount(
            @Parameter(description = "User's email address", required = true)
            @PathVariable String userEmail) {
        int count = wishlistService.getWishlistItemCount(userEmail);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @Operation(
            summary = "Check if product is in wishlist",
            description = "Checks if a specific product is in the user's wishlist",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Check completed successfully")
            }
    )
    @GetMapping("/{userEmail}/contains/{productId}")
    public ResponseEntity<Map<String, Boolean>> isInWishlist(
            @Parameter(description = "User's email address", required = true)
            @PathVariable String userEmail,
            @Parameter(description = "Product ID to check", required = true)
            @PathVariable String productId) {
        boolean inWishlist = wishlistService.isInWishlist(userEmail, productId);
        return ResponseEntity.ok(Map.of("inWishlist", inWishlist));
    }

    @Operation(
            summary = "Move wishlist to cart",
            description = "Moves all items from wishlist to cart",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Items moved to cart successfully"),
                    @ApiResponse(responseCode = "404", description = "Wishlist not found")
            }
    )
    @PostMapping("/{userEmail}/move-to-cart")
    public ResponseEntity<Map<String, List<String>>> moveToCart(
            @Parameter(description = "User's email address", required = true)
            @PathVariable String userEmail) {
        List<String> productIds = wishlistService.moveToCart(userEmail);
        return ResponseEntity.ok(Map.of("movedProductIds", productIds));
    }

    @Operation(
            summary = "Refresh stock status",
            description = "Updates stock availability for all wishlist items",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Stock status refreshed successfully")
            }
    )
    @PostMapping("/{userEmail}/refresh")
    public ResponseEntity<Void> refreshStockStatus(
            @Parameter(description = "User's email address", required = true)
            @PathVariable String userEmail) {
        wishlistService.refreshStockStatus(userEmail);
        return ResponseEntity.noContent().build();
    }
}
