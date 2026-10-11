package com.listings.search.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A listing exactly as received from a feed. Immutable domain object.
 * {@code id} is only unique within a {@code source}; use {@link #key()} for global identity.
 */
public record Listing(
        String id,
        String source,
        String address,
        String city,
        String state,
        String zip,
        BigDecimal price,
        int bedrooms,
        double bathrooms,
        int sqft,
        double latitude,
        double longitude,
        LocalDate listedDate,
        ListingStatus status,
        String description) {

    /** Globally unique key: {@code source:id}. */
    public String key() {
        return source + ":" + id;
    }
}
