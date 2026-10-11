package com.listings.search.model;

import java.util.List;

/** A listing that matched the filters, with its score and any likely cross-feed duplicates. */
public record ScoredListing(Listing listing, RelevanceScore score, List<Listing> possibleDuplicates) {
}
