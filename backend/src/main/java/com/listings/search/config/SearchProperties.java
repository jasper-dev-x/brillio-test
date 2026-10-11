package com.listings.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import com.listings.search.dto.ListingSearchRequest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Search behaviour settings ({@code listings.search.*}). */
@Validated
@ConfigurationProperties(prefix = "listings.search")
public record SearchProperties(
        @DefaultValue("5") @Min(1) @Max(ListingSearchRequest.MAX_PAGE_SIZE) int defaultPageSize) {

    public static SearchProperties defaults() {
        return new SearchProperties(5);
    }
}
