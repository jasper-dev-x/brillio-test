package com.listings.search;

import static com.listings.search.TestListings.TODAY;
import static com.listings.search.TestListings.listing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.listings.model.Listing;

class DuplicateDetectorTest {

    private final DuplicateDetector detector = new DuplicateDetector();

    @Test
    void normalizesSuffixesUnitsAndPunctuation() {
        assertEquals(DuplicateDetector.normalizeAddress("123 Main St, Apt 4B"),
                DuplicateDetector.normalizeAddress("123 Main Street, Unit 4B"));
        assertEquals(DuplicateDetector.normalizeAddress("55 Elm Ct"),
                DuplicateDetector.normalizeAddress("55 ELM COURT"));
        assertEquals(DuplicateDetector.normalizeAddress("9 Oak Ave #2"),
                DuplicateDetector.normalizeAddress("9 Oak Avenue Suite 2"));
    }

    @Test
    void flagsSamePropertyAcrossSources() {
        Listing a = listing("A1", "MLS_A", "123 Main St, Apt 4B", "Springfield", 450_000, 2, TODAY, "active", "");
        Listing b = listing("B7", "MLS_B", "123 Main Street, Unit 4B", "springfield ", 452_000, 2, TODAY, "active", "");
        Map<String, List<Listing>> dups = detector.findDuplicates(List.of(a, b));
        assertEquals(List.of(b), dups.get(a.key()));
        assertEquals(List.of(a), dups.get(b.key()));
    }

    @Test
    void differentUnitsInSameBuildingAreNotDuplicates() {
        Listing a = listing("A1", "MLS_A", "123 Main St, Apt 4B", "Springfield", 450_000, 2, TODAY, "active", "");
        Listing b = listing("B7", "MLS_B", "123 Main St, Apt 5C", "Springfield", 450_000, 2, TODAY, "active", "");
        Map<String, List<Listing>> dups = detector.findDuplicates(List.of(a, b));
        assertTrue(dups.get(a.key()).isEmpty());
        assertTrue(dups.get(b.key()).isEmpty());
    }

    @Test
    void sameAddressInDifferentCityIsNotDuplicate() {
        Listing a = listing("A1", "MLS_A", "10 Pine Rd", "Fairfax", 400_000, 2, TODAY, "active", "");
        Listing b = listing("B1", "MLS_B", "10 Pine Rd", "Reston", 400_000, 2, TODAY, "active", "");
        assertTrue(detector.findDuplicates(List.of(a, b)).get(a.key()).isEmpty());
    }

    @Test
    void sameIdInDifferentSourcesIsKeptDistinct() {
        // ids are only unique per source; keys must not collide
        Listing a = listing("X1", "MLS_A", "1 First St", "Vienna", 1, 1, TODAY, "active", "");
        Listing b = listing("X1", "MLS_B", "2 Second St", "Vienna", 1, 1, TODAY, "active", "");
        Map<String, List<Listing>> dups = detector.findDuplicates(List.of(a, b));
        assertEquals(2, dups.size());
    }
}
