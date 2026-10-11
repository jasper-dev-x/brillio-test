package com.listings;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ListingSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(ListingSearchApplication.class, args);
    }

    /**
     * Recency scoring depends on "today". Injecting a Clock (instead of calling
     * LocalDate.now() inline) keeps the scorer deterministic under test.
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
