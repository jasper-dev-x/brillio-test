package com.listings.search;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import com.listings.model.Listing;
import com.listings.repository.ListingRepository;

/** Shared fixtures for unit tests. */
final class TestListings {

    /** "Today" for every test: recency math is pinned to this date. */
    static final LocalDate TODAY = LocalDate.of(2026, 9, 5);
    static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private TestListings() {
    }

    static Listing listing(String id, String source, String address, String city,
                           long price, int bedrooms, LocalDate listedDate, String status, String description) {
        return new Listing(id, source, address, city, "VA", "22150", price, bedrooms, 2.0, 1200,
                38.8, -77.2, listedDate, status, description);
    }

    static Listing listing(String id, long price, int bedrooms, String city, LocalDate listedDate) {
        return listing(id, "MLS_A", id + " Test St", city, price, bedrooms, listedDate, "active", "Nice home");
    }

    static SearchService service(List<Listing> listings) {
        return new SearchService(new ListingRepository(listings), new SearchValidator(),
                new RelevanceScorer(CLOCK), new DuplicateDetector());
    }

    /** Fluent builder so tests only spell out the criteria they care about. */
    static Criteria criteria() {
        return new Criteria();
    }

    static final class Criteria {
        private Long minPrice, maxPrice, targetBudget;
        private Integer minBedrooms, page, pageSize;
        private String city, keyword, status;

        Criteria minPrice(long v) { minPrice = v; return this; }
        Criteria maxPrice(long v) { maxPrice = v; return this; }
        Criteria targetBudget(long v) { targetBudget = v; return this; }
        Criteria minBedrooms(int v) { minBedrooms = v; return this; }
        Criteria page(int v) { page = v; return this; }
        Criteria pageSize(int v) { pageSize = v; return this; }
        Criteria city(String v) { city = v; return this; }
        Criteria keyword(String v) { keyword = v; return this; }
        Criteria status(String v) { status = v; return this; }

        SearchCriteria build() {
            return new SearchCriteria(minPrice, maxPrice, minBedrooms, city, keyword, targetBudget, status, page, pageSize);
        }
    }
}
