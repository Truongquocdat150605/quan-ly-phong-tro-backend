package com.example.quanliPT.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.annotation.EnableCaching;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // Cache đơn giản, không cần thêm dependency.
        // Có thể đổi sang Caffeine/Ehcache nếu bạn muốn nâng cấp sau.
        return new ConcurrentMapCacheManager(
                "roomsAvailable",
                "roomsById",
                "roomsHot",
                "roomsNewest"
        );
    }
}
