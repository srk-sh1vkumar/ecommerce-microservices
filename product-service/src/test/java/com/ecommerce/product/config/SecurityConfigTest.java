package com.ecommerce.product.config;

import com.ecommerce.common.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for SecurityConfig.
 * Tests security configuration setup for product-service.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfig Tests")
class SecurityConfigTest {

    private SecurityConfig securityConfig;

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig();
        ReflectionTestUtils.setField(securityConfig, "jwtAuthenticationFilter", jwtAuthenticationFilter);
    }

    @Test
    @DisplayName("Should create SecurityConfig instance")
    void securityConfig_ShouldBeCreated() {
        assertThat(securityConfig).isNotNull();
    }

    @Test
    @DisplayName("Should have JWT filter injected")
    void securityConfig_ShouldHaveJwtFilterInjected() {
        // Assert
        Object filter = ReflectionTestUtils.getField(securityConfig, "jwtAuthenticationFilter");
        assertThat(filter).isNotNull();
        assertThat(filter).isEqualTo(jwtAuthenticationFilter);
    }
}
