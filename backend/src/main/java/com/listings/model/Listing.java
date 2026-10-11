package com.listings.model;

import java.time.LocalDate;

/**
 * A single listing exactly as it arrives from a feed. Immutable.
 * Note: {@code id} is only unique within a {@code source}; use {@link #key()} for global identity.
 */
public record Listing(
        String id,
        String source,
        String address,
        String city,
        String state,
        String zip,
        long price,
        int bedrooms,
        double bathrooms,
        int sqft,
        double latitude,
        double longitude,
        LocalDate listedDate,
        String status,
        String description) {

    /** Globally unique key: source + id. */
    public String key() {
        return source + ":" + id;
    }
}
