package com.listings.search;

import java.time.LocalDate;
import java.util.List;

/** One page of ranked results plus pagination metadata. This is the API response body. */
public record SearchResult(
        List<Item> items,
        int page,
        int pageSize,
        int totalItems,
        int totalPages,
        List<String> notices) {

    /** A listing as returned to clients: listing fields + score + duplicate flags. */
    public record Item(
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
            String description,
            double score,
            ScoreBreakdown scoreBreakdown,
            List<DuplicateRef> possibleDuplicates) {
    }

    /** @param budget null when no targetBudget was supplied */
    public record ScoreBreakdown(Double budget, double recency) {
    }

    public record DuplicateRef(String id, String source, String address, long price) {
    }
}
