package com.ecommerce.product.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CircuitBreakerConfiguration.
 * Tests circuit breaker registry and individual circuit breaker configurations.
 */
@DisplayName("CircuitBreakerConfiguration Tests")
class CircuitBreakerConfigurationTest {

    private CircuitBreakerConfiguration config;

    @BeforeEach
    void setUp() {
        config = new CircuitBreakerConfiguration();
    }

    @Test
    @DisplayName("Should create circuit breaker registry")
    void circuitBreakerRegistry_ShouldBeCreated() {
        // Act
        CircuitBreakerRegistry registry = config.circuitBreakerRegistry();

        // Assert
        assertThat(registry).isNotNull();
    }

    @Test
    @DisplayName("Should create product service circuit breaker")
    void productServiceCircuitBreaker_ShouldBeCreated() {
        // Arrange
        CircuitBreakerRegistry registry = config.circuitBreakerRegistry();

        // Act
        CircuitBreaker circuitBreaker = config.productServiceCircuitBreaker(registry);

        // Assert
        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.getName()).isEqualTo("productService");
    }

    @Test
    @DisplayName("Should create database circuit breaker")
    void databaseCircuitBreaker_ShouldBeCreated() {
        // Arrange
        CircuitBreakerRegistry registry = config.circuitBreakerRegistry();

        // Act
        CircuitBreaker circuitBreaker = config.databaseCircuitBreaker(registry);

        // Assert
        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.getName()).isEqualTo("database");
    }

    @Test
    @DisplayName("Should configure registry with failure rate threshold")
    void circuitBreakerRegistry_ShouldHaveCorrectConfiguration() {
        // Act
        CircuitBreakerRegistry registry = config.circuitBreakerRegistry();
        CircuitBreaker defaultCircuitBreaker = registry.circuitBreaker("test");

        // Assert
        assertThat(defaultCircuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold())
            .isEqualTo(50.0f);
    }

    @Test
    @DisplayName("Should configure database circuit breaker with custom config")
    void databaseCircuitBreaker_ShouldHaveCustomConfiguration() {
        // Arrange
        CircuitBreakerRegistry registry = config.circuitBreakerRegistry();

        // Act
        CircuitBreaker circuitBreaker = config.databaseCircuitBreaker(registry);

        // Assert
        assertThat(circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold())
            .isEqualTo(60.0f);
        assertThat(circuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize())
            .isEqualTo(20);
        assertThat(circuitBreaker.getCircuitBreakerConfig().getMinimumNumberOfCalls())
            .isEqualTo(10);
    }

    @Test
    @DisplayName("Should configure default circuit breaker with sliding window")
    void circuitBreakerRegistry_ShouldHaveSlidingWindow() {
        // Act
        CircuitBreakerRegistry registry = config.circuitBreakerRegistry();
        CircuitBreaker defaultCircuitBreaker = registry.circuitBreaker("test");

        // Assert
        assertThat(defaultCircuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize())
            .isEqualTo(10);
    }
}
