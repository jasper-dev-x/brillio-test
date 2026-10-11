package com.listings.search.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** A ranked listing as returned to clients: listing fields, relevance score and duplicate flags. */
public record ListingResponse(
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
        String status,
        String description,
        double score,
        ScoreBreakdown scoreBreakdown,
        List<DuplicateRef> possibleDuplicates) {

    /** @param budget null when no targetBudget was supplied */
    public record ScoreBreakdown(Double budget, double recency) {
    }

    public record DuplicateRef(String id, String source, String address, BigDecimal price) {
    }
}
