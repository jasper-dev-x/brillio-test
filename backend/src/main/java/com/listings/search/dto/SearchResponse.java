package com.listings.search.dto;

import java.util.List;

/** Response body for {@code GET /api/v1/listings}. */
public record SearchResponse(
        List<ListingResponse> items,
        int page,
        int pageSize,
        int totalItems,
        int totalPages,
        List<String> notices) {
}
