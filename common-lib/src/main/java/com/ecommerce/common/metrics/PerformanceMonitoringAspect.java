package com.ecommerce.common.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AOP-based performance monitoring aspect.
 *
 * Features:
 * - Automatic method execution timing
 * - Slow query detection (>1 second)
 * - Performance degradation alerts
 * - Method-level metrics collection
 *
 * Usage:
 * Annotate methods with @MonitorPerformance
 *
 * @author E-commerce Development Team
 * @version 2.0
 */
@Aspect
@Component
public class PerformanceMonitoringAspect {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringAspect.class);
    private static final long SLOW_THRESHOLD_MS = 1000; // 1 second

    private final MeterRegistry meterRegistry;

    public PerformanceMonitoringAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Around("@annotation(monitorPerformance)")
    public Object monitorMethodPerformance(ProceedingJoinPoint joinPoint,
                                          MonitorPerformance monitorPerformance) throws Throwable {

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String metricName = "ecommerce.method.execution";

        Timer.Sample sample = Timer.start(meterRegistry);
        long startTime = System.currentTimeMillis();
        Throwable exception = null;

        try {
            Object result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            exception = e;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // Record timing metric
            Timer timer = Timer.builder(metricName)
                    .description("Method execution time")
                    .tag("class", className)
                    .tag("method", methodName)
                    .tag("status", exception == null ? "success" : "error")
                    .register(meterRegistry);

            sample.stop(timer);

            // Log slow methods
            if (duration > SLOW_THRESHOLD_MS) {
                logger.warn("SLOW METHOD DETECTED: {}.{} took {}ms (threshold: {}ms)",
                        className, methodName, duration, SLOW_THRESHOLD_MS);

                meterRegistry.counter("ecommerce.method.slow",
                        "class", className,
                        "method", methodName).increment();
            }

            // Log performance details if enabled
            if (monitorPerformance.logPerformance()) {
                logger.info("Performance: {}.{} executed in {}ms",
                        className, methodName, duration);
            }
        }
    }

    @Around("@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public Object monitorRestEndpoints(ProceedingJoinPoint joinPoint) throws Throwable {

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String endpoint = className + "." + methodName;

        Timer.Sample sample = Timer.start(meterRegistry);
        long startTime = System.currentTimeMillis();
        boolean success = true;

        try {
            Object result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            success = false;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // Record endpoint timing
            Timer timer = Timer.builder("ecommerce.api.request.duration")
                    .description("API endpoint execution time")
                    .tag("endpoint", endpoint)
                    .tag("status", success ? "success" : "error")
                    .register(meterRegistry);

            sample.stop(timer);

            // Record request counter
            meterRegistry.counter("ecommerce.api.requests",
                    "endpoint", endpoint,
                    "status", success ? "success" : "error").increment();

            // Alert on slow endpoints
            if (duration > SLOW_THRESHOLD_MS) {
                logger.warn("SLOW ENDPOINT: {} took {}ms", endpoint, duration);
            }
        }
    }
}