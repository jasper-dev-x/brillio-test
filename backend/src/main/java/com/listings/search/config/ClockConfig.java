package com.listings.search.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Recency scoring depends on "today". Injecting a {@link Clock} instead of calling
 * {@code LocalDate.now()} inline keeps scoring deterministic under test.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
