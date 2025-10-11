package com.ecommerce.cart.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CacheConfig Tests")
class CacheConfigTest {

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    @Test
    @DisplayName("Should create cache manager with Redis")
    void cacheManager_ShouldCreateRedisCacheManager() {
        CacheConfig cacheConfig = new CacheConfig();

        CacheManager cacheManager = cacheConfig.cacheManager(redisConnectionFactory);

        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
    }
}
