package com.listings.search.model;

import java.util.List;

/** One page of ranked results plus paging metadata and user-facing notices. */
public record SearchResult(
        List<ScoredListing> items,
        int page,
        int pageSize,
        int totalItems,
        int totalPages,
        List<String> notices) {
}
