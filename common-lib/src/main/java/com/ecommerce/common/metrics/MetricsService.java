package com.ecommerce.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Centralized metrics service for tracking business and technical metrics.
 * Uses Micrometer for vendor-neutral metric collection.
 */
@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;

    // User metrics
    private final Counter userRegistrations;
    private final Counter userLogins;
    private final Counter failedLogins;

    // Product metrics
    private final Counter productCreations;
    private final Counter productViews;
    private final Counter productSearches;

    // Cart metrics
    private final Counter itemsAddedToCart;
    private final Counter itemsRemovedFromCart;
    private final Counter cartsCleared;

    // Order metrics
    private final Counter ordersPlaced;
    private final Counter ordersFailed;
    private final Timer checkoutDuration;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize user metrics
        this.userRegistrations = Counter.builder("user.registrations")
                .description("Total number of user registrations")
                .tag("type", "authentication")
                .register(meterRegistry);

        this.userLogins = Counter.builder("user.logins.success")
                .description("Total number of successful user logins")
                .tag("type", "authentication")
                .register(meterRegistry);

        this.failedLogins = Counter.builder("user.logins.failed")
                .description("Total number of failed login attempts")
                .tag("type", "authentication")
                .register(meterRegistry);

        // Initialize product metrics
        this.productCreations = Counter.builder("product.creations")
                .description("Total number of products created")
                .tag("type", "catalog")
                .register(meterRegistry);

        this.productViews = Counter.builder("product.views")
                .description("Total number of product views")
                .tag("type", "catalog")
                .register(meterRegistry);

        this.productSearches = Counter.builder("product.searches")
                .description("Total number of product searches")
                .tag("type", "catalog")
                .register(meterRegistry);

        // Initialize cart metrics
        this.itemsAddedToCart = Counter.builder("cart.items.added")
                .description("Total number of items added to cart")
                .tag("type", "cart")
                .register(meterRegistry);

        this.itemsRemovedFromCart = Counter.builder("cart.items.removed")
                .description("Total number of items removed from cart")
                .tag("type", "cart")
                .register(meterRegistry);

        this.cartsCleared = Counter.builder("cart.cleared")
                .description("Total number of carts cleared")
                .tag("type", "cart")
                .register(meterRegistry);

        // Initialize order metrics
        this.ordersPlaced = Counter.builder("orders.placed")
                .description("Total number of orders placed")
                .tag("type", "orders")
                .register(meterRegistry);

        this.ordersFailed = Counter.builder("orders.failed")
                .description("Total number of failed orders")
                .tag("type", "orders")
                .register(meterRegistry);

        this.checkoutDuration = Timer.builder("checkout.duration")
                .description("Time taken to complete checkout")
                .tag("type", "orders")
                .register(meterRegistry);
    }

    // User metrics
    public void incrementUserRegistrations() {
        userRegistrations.increment();
    }

    public void incrementUserLogins() {
        userLogins.increment();
    }

    public void incrementFailedLogins() {
        failedLogins.increment();
    }

    // Product metrics
    public void incrementProductCreations() {
        productCreations.increment();
    }

    public void incrementProductViews() {
        productViews.increment();
    }

    public void incrementProductSearches() {
        productSearches.increment();
    }

    // Cart metrics
    public void incrementItemsAddedToCart() {
        itemsAddedToCart.increment();
    }

    public void incrementItemsRemovedFromCart() {
        itemsRemovedFromCart.increment();
    }

    public void incrementCartsCleared() {
        cartsCleared.increment();
    }

    // Order metrics
    public void incrementOrdersPlaced() {
        ordersPlaced.increment();
    }

    public void incrementOrdersFailed() {
        ordersFailed.increment();
    }

    public void recordCheckoutDuration(long duration, TimeUnit unit) {
        checkoutDuration.record(duration, unit);
    }

    /**
     * Gets the meter registry for custom metrics.
     */
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }

    /**
     * Increments a custom counter.
     */
    public void incrementCustomCounter(String name, String... tags) {
        Counter.builder(name)
                .tags(tags)
                .register(meterRegistry)
                .increment();
    }
}
