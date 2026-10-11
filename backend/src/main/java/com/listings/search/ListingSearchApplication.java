package com.listings.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ListingSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(ListingSearchApplication.class, args);
    }
}
