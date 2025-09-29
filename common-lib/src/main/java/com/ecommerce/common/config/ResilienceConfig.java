package com.ecommerce.common.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Common resilience configuration for circuit breakers, retries, and timeouts.
 * Provides consistent resilience patterns across all microservices.
 *
 * Features:
 * - Circuit breaker for database operations
 * - Circuit breaker for external service calls
 * - Retry configuration with exponential backoff
 * - Timeout configuration
 *
 * @author E-commerce Development Team
 * @version 2.0
 */
@Configuration
public class ResilienceConfig {

    /**
     * Circuit breaker configuration for database operations.
     * More permissive as database should be highly available.
     */
    @Bean
    public CircuitBreakerConfig databaseCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)  // Open circuit if 50% of requests fail
                .waitDurationInOpenState(Duration.ofSeconds(30))  // Wait 30s before trying again
                .slidingWindowSize(10)  // Consider last 10 requests
                .minimumNumberOfCalls(5)  // Need at least 5 calls before calculating rate
                .permittedNumberOfCallsInHalfOpenState(3)  // Try 3 calls in half-open state
                .slowCallDurationThreshold(Duration.ofSeconds(2))  // Call is slow if > 2s
                .slowCallRateThreshold(50)  // Open if 50% of calls are slow
                .build();
    }

    /**
     * Circuit breaker configuration for external service calls.
     * More aggressive as external services may be unreliable.
     */
    @Bean
    public CircuitBreakerConfig externalServiceCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(30)  // Open circuit if 30% of requests fail
                .waitDurationInOpenState(Duration.ofSeconds(60))  // Wait 60s before trying again
                .slidingWindowSize(20)  // Consider last 20 requests
                .minimumNumberOfCalls(10)  // Need at least 10 calls
                .permittedNumberOfCallsInHalfOpenState(5)  // Try 5 calls in half-open state
                .slowCallDurationThreshold(Duration.ofSeconds(3))  // Call is slow if > 3s
                .slowCallRateThreshold(40)  // Open if 40% of calls are slow
                .build();
    }

    /**
     * Retry configuration with exponential backoff.
     * Suitable for transient failures.
     */
    @Bean
    public RetryConfig retryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3)  // Retry up to 3 times
                .waitDuration(Duration.ofMillis(500))  // Initial wait 500ms
                .retryExceptions(Exception.class)  // Retry on any exception
                .ignoreExceptions(IllegalArgumentException.class)  // Don't retry validation errors
                .build();
    }

    /**
     * Time limiter configuration for async operations.
     */
    @Bean
    public TimeLimiterConfig timeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5))  // Timeout after 5 seconds
                .cancelRunningFuture(true)  // Cancel the running future on timeout
                .build();
    }
}