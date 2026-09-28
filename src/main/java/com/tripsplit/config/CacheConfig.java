package com.tripsplit.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    // Uses Spring's default simple ConcurrentMapCache
    // configured via application.yml
}
