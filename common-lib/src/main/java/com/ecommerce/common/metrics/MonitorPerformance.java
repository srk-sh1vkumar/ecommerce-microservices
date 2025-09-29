package com.ecommerce.common.metrics;

import java.lang.annotation.*;

/**
 * Annotation to mark methods for performance monitoring.
 *
 * Usage:
 * {@code
 * @MonitorPerformance
 * public void expensiveOperation() {
 *     // method implementation
 * }
 * }
 *
 * Features:
 * - Automatic execution time tracking
 * - Slow method detection
 * - Custom metrics collection
 *
 * @author E-commerce Development Team
 * @version 2.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MonitorPerformance {

    /**
     * Whether to log performance details.
     */
    boolean logPerformance() default false;

    /**
     * Custom metric name (optional).
     */
    String metricName() default "";
}