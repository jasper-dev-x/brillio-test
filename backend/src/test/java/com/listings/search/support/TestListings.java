package com.listings.search.support;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import com.listings.search.config.ScoringProperties;
import com.listings.search.model.Listing;
import com.listings.search.model.ListingStatus;
import com.listings.search.service.DefaultListingSearchService;
import com.listings.search.service.dedup.DuplicateDetector;
import com.listings.search.service.scoring.RelevanceScorer;

/** Shared fixtures for unit tests (no Spring context). */
public final class TestListings {

    /** "Today" for every unit test; recency math is pinned to this date. */
    public static final LocalDate TODAY = LocalDate.of(2026, 9, 5);
    public static final Clock FIXED_CLOCK = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private TestListings() {
    }

    public static ListingBuilder listing(String id) {
        return new ListingBuilder(id);
    }

    public static RelevanceScorer scorer() {
        return new RelevanceScorer(FIXED_CLOCK, ScoringProperties.defaults());
    }

    public static DefaultListingSearchService service(List<Listing> listings) {
        return new DefaultListingSearchService(() -> listings, scorer(), new DuplicateDetector());
    }

    /** Builds a listing with sensible defaults; tests override only what they care about. */
    public static final class ListingBuilder {
        private final String id;
        private String source = "MLS_A";
        private String address;
        private String city = "Reston";
        private long price = 500_000;
        private int bedrooms = 3;
        private LocalDate listedDate = TODAY;
        private ListingStatus status = ListingStatus.ACTIVE;
        private String description = "Nice home";

        private ListingBuilder(String id) {
            this.id = id;
            this.address = id + " Test St";
        }

        public ListingBuilder source(String v) { this.source = v; return this; }
        public ListingBuilder address(String v) { this.address = v; return this; }
        public ListingBuilder city(String v) { this.city = v; return this; }
        public ListingBuilder price(long v) { this.price = v; return this; }
        public ListingBuilder bedrooms(int v) { this.bedrooms = v; return this; }
        public ListingBuilder listedDate(LocalDate v) { this.listedDate = v; return this; }
        public ListingBuilder daysAgo(int days) { this.listedDate = TODAY.minusDays(days); return this; }
        public ListingBuilder status(ListingStatus v) { this.status = v; return this; }
        public ListingBuilder description(String v) { this.description = v; return this; }

        public Listing build() {
            return new Listing(id, source, address, city, "VA", "22150", BigDecimal.valueOf(price), bedrooms, 2.0,
                    1200, 38.8, -77.2, listedDate, status, description);
        }
    }
}
