package com.ecommerce.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Custom business metrics for application monitoring.
 *
 * Tracked Metrics:
 * - User registrations and logins
 * - Product views and searches
 * - Cart operations
 * - Order placements and revenue
 * - API performance timings
 * - Error rates by type
 *
 * Integration:
 * - Prometheus metrics endpoint: /actuator/prometheus
 * - Grafana dashboards compatible
 * - AppDynamics custom metrics
 *
 * @author E-commerce Development Team
 * @version 2.0
 */
@Component
public class PerformanceMetrics {

    private final MeterRegistry meterRegistry;

    // User metrics
    private final Counter userRegistrations;
    private final Counter userLogins;
    private final Counter loginFailures;

    // Product metrics
    private final Counter productViews;
    private final Counter productSearches;
    private final Timer productSearchDuration;

    // Cart metrics
    private final Counter cartAdditions;
    private final Counter cartRemovals;
    private final Counter cartCheckouts;

    // Order metrics
    private final Counter orderPlacements;
    private final Counter orderCompletions;
    private final Counter orderCancellations;
    private final Timer orderProcessingTime;

    // Error metrics
    private final Counter validationErrors;
    private final Counter authenticationErrors;
    private final Counter serverErrors;
    private final Counter rateLimitExceeded;

    public PerformanceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize user metrics
        this.userRegistrations = Counter.builder("ecommerce.user.registrations")
                .description("Total user registrations")
                .tag("service", "user")
                .register(meterRegistry);

        this.userLogins = Counter.builder("ecommerce.user.logins")
                .description("Total successful user logins")
                .tag("service", "user")
                .register(meterRegistry);

        this.loginFailures = Counter.builder("ecommerce.user.login.failures")
                .description("Total failed login attempts")
                .tag("service", "user")
                .register(meterRegistry);

        // Initialize product metrics
        this.productViews = Counter.builder("ecommerce.product.views")
                .description("Total product views")
                .tag("service", "product")
                .register(meterRegistry);

        this.productSearches = Counter.builder("ecommerce.product.searches")
                .description("Total product searches")
                .tag("service", "product")
                .register(meterRegistry);

        this.productSearchDuration = Timer.builder("ecommerce.product.search.duration")
                .description("Product search duration")
                .tag("service", "product")
                .register(meterRegistry);

        // Initialize cart metrics
        this.cartAdditions = Counter.builder("ecommerce.cart.additions")
                .description("Total items added to cart")
                .tag("service", "cart")
                .register(meterRegistry);

        this.cartRemovals = Counter.builder("ecommerce.cart.removals")
                .description("Total items removed from cart")
                .tag("service", "cart")
                .register(meterRegistry);

        this.cartCheckouts = Counter.builder("ecommerce.cart.checkouts")
                .description("Total cart checkouts initiated")
                .tag("service", "cart")
                .register(meterRegistry);

        // Initialize order metrics
        this.orderPlacements = Counter.builder("ecommerce.order.placements")
                .description("Total orders placed")
                .tag("service", "order")
                .register(meterRegistry);

        this.orderCompletions = Counter.builder("ecommerce.order.completions")
                .description("Total orders completed")
                .tag("service", "order")
                .register(meterRegistry);

        this.orderCancellations = Counter.builder("ecommerce.order.cancellations")
                .description("Total orders cancelled")
                .tag("service", "order")
                .register(meterRegistry);

        this.orderProcessingTime = Timer.builder("ecommerce.order.processing.time")
                .description("Order processing time")
                .tag("service", "order")
                .register(meterRegistry);

        // Initialize error metrics
        this.validationErrors = Counter.builder("ecommerce.errors.validation")
                .description("Total validation errors")
                .tag("type", "validation")
                .register(meterRegistry);

        this.authenticationErrors = Counter.builder("ecommerce.errors.authentication")
                .description("Total authentication errors")
                .tag("type", "authentication")
                .register(meterRegistry);

        this.serverErrors = Counter.builder("ecommerce.errors.server")
                .description("Total server errors (5xx)")
                .tag("type", "server")
                .register(meterRegistry);

        this.rateLimitExceeded = Counter.builder("ecommerce.errors.ratelimit")
                .description("Total rate limit exceeded errors")
                .tag("type", "ratelimit")
                .register(meterRegistry);
    }

    // User metric methods
    public void recordUserRegistration() {
        userRegistrations.increment();
    }

    public void recordUserLogin() {
        userLogins.increment();
    }

    public void recordLoginFailure() {
        loginFailures.increment();
    }

    // Product metric methods
    public void recordProductView(String productId) {
        productViews.increment();
        meterRegistry.counter("ecommerce.product.views.by.id",
                "productId", productId).increment();
    }

    public void recordProductSearch(String query) {
        productSearches.increment();
    }

    public void recordProductSearchDuration(long durationMillis) {
        productSearchDuration.record(durationMillis, TimeUnit.MILLISECONDS);
    }

    public Timer.Sample startProductSearchTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopProductSearchTimer(Timer.Sample sample) {
        sample.stop(productSearchDuration);
    }

    // Cart metric methods
    public void recordCartAddition(String productId, int quantity) {
        cartAdditions.increment(quantity);
        meterRegistry.counter("ecommerce.cart.additions.by.product",
                "productId", productId).increment(quantity);
    }

    public void recordCartRemoval(String productId, int quantity) {
        cartRemovals.increment(quantity);
    }

    public void recordCartCheckout(double totalAmount) {
        cartCheckouts.increment();
        meterRegistry.summary("ecommerce.cart.checkout.value").record(totalAmount);
    }

    // Order metric methods
    public void recordOrderPlacement(String orderId, double amount) {
        orderPlacements.increment();
        meterRegistry.summary("ecommerce.order.value").record(amount);
        meterRegistry.counter("ecommerce.revenue.total").increment(amount);
    }

    public void recordOrderCompletion(String orderId) {
        orderCompletions.increment();
    }

    public void recordOrderCancellation(String orderId) {
        orderCancellations.increment();
    }

    public void recordOrderProcessingTime(long durationMillis) {
        orderProcessingTime.record(durationMillis, TimeUnit.MILLISECONDS);
    }

    public Timer.Sample startOrderProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopOrderProcessingTimer(Timer.Sample sample) {
        sample.stop(orderProcessingTime);
    }

    // Error metric methods
    public void recordValidationError(String errorCode) {
        validationErrors.increment();
        meterRegistry.counter("ecommerce.errors.validation.by.code",
                "errorCode", errorCode).increment();
    }

    public void recordAuthenticationError(String reason) {
        authenticationErrors.increment();
        meterRegistry.counter("ecommerce.errors.authentication.by.reason",
                "reason", reason).increment();
    }

    public void recordServerError(String endpoint, String errorType) {
        serverErrors.increment();
        meterRegistry.counter("ecommerce.errors.server.by.endpoint",
                "endpoint", endpoint, "errorType", errorType).increment();
    }

    public void recordRateLimitExceeded(String clientIp) {
        rateLimitExceeded.increment();
    }

    // Custom gauge methods
    public void recordActiveUsers(int count) {
        meterRegistry.gauge("ecommerce.users.active", count);
    }

    public void recordInventoryLevel(String productId, int quantity) {
        meterRegistry.gauge("ecommerce.inventory.level",
                java.util.Collections.singletonList(io.micrometer.core.instrument.Tag.of("productId", productId)),
                quantity);
    }

    // Business KPI methods
    public void recordConversionRate(double rate) {
        meterRegistry.gauge("ecommerce.kpi.conversion.rate", rate);
    }

    public void recordAverageOrderValue(double value) {
        meterRegistry.gauge("ecommerce.kpi.average.order.value", value);
    }

    public void recordCustomerSatisfaction(double score) {
        meterRegistry.gauge("ecommerce.kpi.customer.satisfaction", score);
    }
}