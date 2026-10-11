package com.listings.search.service.dedup;

import static com.listings.search.support.TestListings.listing;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.listings.search.model.Listing;

class DuplicateDetectorTest {

    private final DuplicateDetector detector = new DuplicateDetector();

    @Test
    void normalizesSuffixesUnitDesignatorsAndPunctuation() {
        assertThat(DuplicateDetector.normalizeAddress("123 Main St, Apt 4B"))
                .isEqualTo(DuplicateDetector.normalizeAddress("123 Main Street, Unit 4B"));
        assertThat(DuplicateDetector.normalizeAddress("55 Elm Ct"))
                .isEqualTo(DuplicateDetector.normalizeAddress("55 ELM COURT"));
        assertThat(DuplicateDetector.normalizeAddress("9 Oak Ave #2"))
                .isEqualTo(DuplicateDetector.normalizeAddress("9 Oak Avenue Suite 2"));
    }

    @Test
    void flagsSamePropertyAcrossFeedsEvenWithDifferentZipFormattingAndCityCase() {
        Listing a = listing("A1").source("MLS_A").address("123 Main St, Apt 4B").city("Springfield").build();
        Listing b = listing("B7").source("MLS_B").address("123 Main Street, Unit 4B").city(" springfield").build();

        Map<String, List<Listing>> duplicates = detector.findDuplicates(List.of(a, b));

        assertThat(duplicates.get(a.key())).containsExactly(b);
        assertThat(duplicates.get(b.key())).containsExactly(a);
    }

    @Test
    void differentUnitsInTheSameBuildingAreNotDuplicates() {
        Listing a = listing("A1").address("123 Main St, Apt 4B").build();
        Listing b = listing("B7").source("MLS_B").address("123 Main St, Apt 5C").build();

        Map<String, List<Listing>> duplicates = detector.findDuplicates(List.of(a, b));

        assertThat(duplicates.get(a.key())).isEmpty();
        assertThat(duplicates.get(b.key())).isEmpty();
    }

    @Test
    void sameStreetAddressInADifferentCityIsNotADuplicate() {
        Listing a = listing("A1").address("10 Pine Rd").city("Fairfax").build();
        Listing b = listing("B1").source("MLS_B").address("10 Pine Rd").city("Reston").build();

        assertThat(detector.findDuplicates(List.of(a, b)).get(a.key())).isEmpty();
    }

    @Test
    void sameIdFromDifferentSourcesDoesNotCollide() {
        Listing a = listing("X1").source("MLS_A").address("1 First St").build();
        Listing b = listing("X1").source("MLS_B").address("2 Second St").build();

        assertThat(detector.findDuplicates(List.of(a, b))).hasSize(2);
    }
}
