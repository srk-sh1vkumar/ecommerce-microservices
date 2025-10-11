package com.ecommerce.cart.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("WebConfig Tests")
class WebConfigTest {

    @Test
    @DisplayName("Should create CORS configuration source")
    void corsConfigurationSource_ShouldCreateCorsConfig() {
        WebConfig webConfig = new WebConfig();

        CorsConfigurationSource source = webConfig.corsConfigurationSource();

        assertThat(source).isNotNull();
        assertThat(source).isInstanceOf(UrlBasedCorsConfigurationSource.class);
    }
}
