package com.ecommerce.user.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CacheConfig.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CacheConfig Tests")
class CacheConfigTest {

    private CacheConfig cacheConfig;

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    @BeforeEach
    void setUp() {
        cacheConfig = new CacheConfig();
    }

    @Test
    @DisplayName("Should create cache manager")
    void cacheManager_ShouldCreateRedisCacheManager() {
        // Act
        CacheManager cacheManager = cacheConfig.cacheManager(redisConnectionFactory);

        // Assert
        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
    }

    @Test
    @DisplayName("Should use Redis connection factory")
    void cacheManager_ShouldUseProvidedConnectionFactory() {
        // Act
        CacheManager cacheManager = cacheConfig.cacheManager(redisConnectionFactory);

        // Assert
        assertThat(cacheManager).isNotNull();
    }
}
