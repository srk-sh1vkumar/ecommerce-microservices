package com.ecommerce.product.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for WebConfig.
 * Tests CORS configuration settings for product-service.
 */
@DisplayName("WebConfig Tests")
class WebConfigTest {

    private WebConfig webConfig;
    private CorsConfigurationSource corsConfigurationSource;

    @BeforeEach
    void setUp() {
        webConfig = new WebConfig();
        corsConfigurationSource = webConfig.corsConfigurationSource();
    }

    @Test
    @DisplayName("Should create CORS configuration source")
    void corsConfigurationSource_ShouldNotBeNull() {
        assertThat(corsConfigurationSource).isNotNull();
        assertThat(corsConfigurationSource).isInstanceOf(UrlBasedCorsConfigurationSource.class);
    }

    @Test
    @DisplayName("Should configure allowed origins")
    void corsConfiguration_ShouldHaveAllowedOrigins() {
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;
        CorsConfiguration config = source.getCorsConfigurations().get("/**");

        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins()).isNotNull();
        assertThat(config.getAllowedOrigins())
            .containsExactlyInAnyOrder(
                "http://localhost:3000",
                "http://localhost:4200",
                "http://localhost:8080"
            );
    }

    @Test
    @DisplayName("Should configure allowed methods")
    void corsConfiguration_ShouldHaveAllowedMethods() {
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;
        CorsConfiguration config = source.getCorsConfigurations().get("/**");

        assertThat(config.getAllowedMethods())
            .containsExactlyInAnyOrder("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }

    @Test
    @DisplayName("Should configure allowed headers")
    void corsConfiguration_ShouldHaveAllowedHeaders() {
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;
        CorsConfiguration config = source.getCorsConfigurations().get("/**");

        assertThat(config.getAllowedHeaders())
            .containsExactlyInAnyOrder("Authorization", "Content-Type", "X-Requested-With");
    }

    @Test
    @DisplayName("Should configure exposed headers")
    void corsConfiguration_ShouldHaveExposedHeaders() {
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;
        CorsConfiguration config = source.getCorsConfigurations().get("/**");

        assertThat(config.getExposedHeaders())
            .containsExactlyInAnyOrder("X-Total-Count", "X-Page-Number");
    }

    @Test
    @DisplayName("Should allow credentials")
    void corsConfiguration_ShouldAllowCredentials() {
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;
        CorsConfiguration config = source.getCorsConfigurations().get("/**");

        assertThat(config.getAllowCredentials()).isTrue();
    }

    @Test
    @DisplayName("Should set max age to 3600 seconds")
    void corsConfiguration_ShouldHaveMaxAge() {
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;
        CorsConfiguration config = source.getCorsConfigurations().get("/**");

        assertThat(config.getMaxAge()).isEqualTo(3600L);
    }
}
