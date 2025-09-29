package com.ecommerce.common.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Helper class for enhanced distributed tracing operations.
 *
 * Features:
 * - Business context in spans
 * - Custom tags and events
 * - Automatic error tracking
 * - Performance annotations
 *
 * Usage Examples:
 * {@code
 * // Create a custom span
 * tracingHelper.executeInSpan("process-order", () -> {
 *     processOrder(orderId);
 *     return orderId;
 * });
 *
 * // Add business context
 * tracingHelper.addTag("userId", userId);
 * tracingHelper.addTag("orderTotal", totalAmount);
 * }
 *
 * @author E-commerce Development Team
 * @version 2.0
 */
@Component
public class TracingHelper {

    private static final Logger logger = LoggerFactory.getLogger(TracingHelper.class);

    private final Tracer tracer;

    public TracingHelper(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * Execute code within a custom span.
     */
    public <T> T executeInSpan(String spanName, Supplier<T> operation) {
        Span span = tracer.nextSpan().name(spanName).start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            T result = operation.get();
            return result;
        } catch (Exception e) {
            span.error(e);
            span.tag("error", "true");
            span.tag("error.message", e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Execute code within a custom span (void return).
     */
    public void executeInSpanVoid(String spanName, Runnable operation) {
        Span span = tracer.nextSpan().name(spanName).start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            operation.run();
        } catch (Exception e) {
            span.error(e);
            span.tag("error", "true");
            span.tag("error.message", e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Add custom tag to current span.
     */
    public void addTag(String key, String value) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            currentSpan.tag(key, value);
        }
    }

    /**
     * Add multiple tags to current span.
     */
    public void addTags(Map<String, String> tags) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            tags.forEach(currentSpan::tag);
        }
    }

    /**
     * Add business context to current span.
     */
    public void addBusinessContext(String userId, String sessionId, String correlationId) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            currentSpan.tag("user.id", userId);
            currentSpan.tag("session.id", sessionId);
            currentSpan.tag("correlation.id", correlationId);
        }
    }

    /**
     * Add user context to current span.
     */
    public void addUserContext(String userId, String email, String role) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            currentSpan.tag("user.id", userId);
            currentSpan.tag("user.email", email);
            currentSpan.tag("user.role", role);
        }
    }

    /**
     * Add product context to current span.
     */
    public void addProductContext(String productId, String category, String price) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            currentSpan.tag("product.id", productId);
            currentSpan.tag("product.category", category);
            currentSpan.tag("product.price", price);
        }
    }

    /**
     * Add order context to current span.
     */
    public void addOrderContext(String orderId, String status, String totalAmount) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            currentSpan.tag("order.id", orderId);
            currentSpan.tag("order.status", status);
            currentSpan.tag("order.total", totalAmount);
        }
    }

    /**
     * Add custom event to current span.
     */
    public void addEvent(String eventName) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            currentSpan.event(eventName);
        }
    }

    /**
     * Add custom event with attributes.
     */
    public void addEvent(String eventName, Map<String, String> attributes) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            currentSpan.event(eventName);
            attributes.forEach(currentSpan::tag);
        }
    }

    /**
     * Record error in current span.
     */
    public void recordError(Throwable error) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            currentSpan.error(error);
            currentSpan.tag("error", "true");
            currentSpan.tag("error.type", error.getClass().getName());
            currentSpan.tag("error.message", error.getMessage());

            // Add stack trace summary
            StackTraceElement[] stackTrace = error.getStackTrace();
            if (stackTrace.length > 0) {
                currentSpan.tag("error.location",
                    stackTrace[0].getClassName() + "." +
                    stackTrace[0].getMethodName() + ":" +
                    stackTrace[0].getLineNumber());
            }
        }
    }

    /**
     * Record performance metric in current span.
     */
    public void recordPerformance(String operationName, long durationMillis) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            currentSpan.tag("performance." + operationName, String.valueOf(durationMillis) + "ms");

            if (durationMillis > 1000) {
                currentSpan.tag("performance.slow", "true");
                logger.warn("Slow operation detected: {} took {}ms", operationName, durationMillis);
            }
        }
    }

    /**
     * Get current trace ID.
     */
    public String getCurrentTraceId() {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            return currentSpan.context().traceId();
        }
        return null;
    }

    /**
     * Get current span ID.
     */
    public String getCurrentSpanId() {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            return currentSpan.context().spanId();
        }
        return null;
    }

    /**
     * Check if tracing is active.
     */
    public boolean isTracingActive() {
        return tracer.currentSpan() != null;
    }
}