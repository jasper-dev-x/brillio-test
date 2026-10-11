package com.listings.search;

import static com.listings.search.TestListings.TODAY;
import static com.listings.search.TestListings.criteria;
import static com.listings.search.TestListings.listing;
import static com.listings.search.TestListings.service;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.listings.model.Listing;

class SearchServiceTest {

    private static List<String> ids(SearchResult r) {
        return r.items().stream().map(SearchResult.Item::id).toList();
    }

    @Nested
    class Filtering {

        private final SearchService svc = service(List.of(
                listing("L1", "MLS_A", "1 A St", "Reston", 400_000, 2, TODAY, "active", "Pet friendly condo with pool"),
                listing("L2", "MLS_A", "2 B St", "Reston", 500_000, 3, TODAY, "pending", "Townhome, community POOL"),
                listing("L3", "MLS_B", "3 C St", "Vienna", 600_000, 4, TODAY, "active", "Large yard, no pets"),
                listing("L4", "MLS_B", "4 D St", "Fairfax", 700_000, 5, TODAY, "sold", "Garage and pool")));

        @Test
        void noFiltersReturnsEverything() {
            assertEquals(4, svc.search(SearchCriteria.empty()).totalItems());
        }

        @Test
        void priceBoundsAreInclusive() {
            SearchResult r = svc.search(criteria().minPrice(500_000).maxPrice(600_000).pageSize(10).build());
            assertEquals(List.of("L2", "L3"), ids(r).stream().sorted().toList());
        }

        @Test
        void minBedroomsIsInclusive() {
            SearchResult r = svc.search(criteria().minBedrooms(4).pageSize(10).build());
            assertEquals(List.of("L3", "L4"), ids(r).stream().sorted().toList());
        }

        @Test
        void cityMatchIsCaseInsensitiveAndTrimmed() {
            SearchResult r = svc.search(criteria().city("  reSTON ").pageSize(10).build());
            assertEquals(2, r.totalItems());
            assertTrue(r.notices().isEmpty());
        }

        @Test
        void keywordIsCaseInsensitiveAndRequiresEveryTerm() {
            assertEquals(3, svc.search(criteria().keyword("Pool").build()).totalItems());
            SearchResult r = svc.search(criteria().keyword("pool  PET").build());
            assertEquals(List.of("L1"), ids(r));
        }

        @Test
        void statusFilterIsCaseInsensitive() {
            assertEquals(List.of("L4"), ids(svc.search(criteria().status("SOLD").build())));
        }

        @Test
        void filtersCombine() {
            SearchResult r = svc.search(criteria().city("Reston").keyword("pool").minBedrooms(3).build());
            assertEquals(List.of("L2"), ids(r));
        }

        @Test
        void noMatchesReturnsEmptyPageNotError() {
            SearchResult r = svc.search(criteria().minPrice(10_000_000).build());
            assertEquals(0, r.totalItems());
            assertEquals(0, r.totalPages());
            assertTrue(r.items().isEmpty());
            assertTrue(r.notices().isEmpty());
        }

        @Test
        void unknownCityReturnsEmptyWithExplanatoryNotice() {
            SearchResult r = svc.search(criteria().city("Atlantis").build());
            assertEquals(0, r.totalItems());
            assertEquals(1, r.notices().size());
            assertTrue(r.notices().get(0).contains("Atlantis"));
            assertTrue(r.notices().get(0).contains("Reston"));
        }

        @Test
        void knownCityFilteredOutByOtherCriteriaHasNoCityNotice() {
            SearchResult r = svc.search(criteria().city("Vienna").minPrice(9_000_000).build());
            assertEquals(0, r.totalItems());
            assertTrue(r.notices().isEmpty());
        }

        @Test
        void blankTextFiltersAreIgnored() {
            assertEquals(4, svc.search(criteria().city("   ").keyword("").build()).totalItems());
        }

        @Test
        void invalidCriteriaIsRejectedBeforeSearching() {
            assertThrows(InvalidSearchException.class,
                    () -> svc.search(criteria().minPrice(5).maxPrice(1).build()));
        }
    }

    @Nested
    class Ranking {

        @Test
        void closerToBudgetRanksHigherWhenRecencyIsEqual() {
            SearchService svc = service(List.of(
                    listing("FAR", 300_000, 3, "Reston", TODAY),
                    listing("EXACT", 500_000, 3, "Reston", TODAY),
                    listing("NEAR", 520_000, 3, "Reston", TODAY)));
            assertEquals(List.of("EXACT", "NEAR", "FAR"), ids(svc.search(criteria().targetBudget(500_000).build())));
        }

        @Test
        void newerRanksHigherWithoutBudget() {
            SearchService svc = service(List.of(
                    listing("OLD", 500_000, 3, "Reston", TODAY.minusDays(60)),
                    listing("NEW", 500_000, 3, "Reston", TODAY),
                    listing("MID", 500_000, 3, "Reston", TODAY.minusDays(10))));
            assertEquals(List.of("NEW", "MID", "OLD"), ids(svc.search(SearchCriteria.empty())));
        }

        @Test
        void tiedScoresBreakByNewestThenLowestPrice() {
            // Both are far over budget (0 budget credit) and listed the same day, so their scores tie.
            // The tie must be broken deterministically: same date -> lower price first.
            SearchService svc = service(List.of(
                    listing("PRICEY", 900_000, 3, "Reston", TODAY),
                    listing("CHEAP", 800_000, 3, "Reston", TODAY)));
            SearchResult r = svc.search(criteria().targetBudget(100_000).build());
            assertEquals(r.items().get(0).score(), r.items().get(1).score());
            assertEquals(List.of("CHEAP", "PRICEY"), ids(r));
        }

        @Test
        void tiedScoresPreferNewerListingOverLowerPrice() {
            // Listings over a year old both round to a 0.01 recency score -> tie; newer must win
            // even though it is more expensive.
            SearchService svc = service(List.of(
                    listing("OLDER_CHEAP", 400_000, 3, "Reston", TODAY.minusDays(401)),
                    listing("OLD_PRICEY", 600_000, 3, "Reston", TODAY.minusDays(400))));
            SearchResult r = svc.search(SearchCriteria.empty());
            assertEquals(r.items().get(0).score(), r.items().get(1).score());
            assertEquals(List.of("OLD_PRICEY", "OLDER_CHEAP"), ids(r));
        }

        @Test
        void fullyIdenticalListingsBreakTiesBySourceThenId() {
            SearchService svc = service(List.of(
                    listing("B", "MLS_B", "1 X St", "Reston", 500_000, 3, TODAY, "active", ""),
                    listing("Z", "MLS_A", "2 X St", "Reston", 500_000, 3, TODAY, "active", ""),
                    listing("A", "MLS_A", "3 X St", "Reston", 500_000, 3, TODAY, "active", "")));
            SearchResult r = svc.search(SearchCriteria.empty());
            assertEquals(List.of("A", "Z", "B"), ids(r));
        }

        @Test
        void sameQueryAlwaysReturnsSameOrder() {
            SearchService svc = service(List.of(
                    listing("A", 500_000, 3, "Reston", TODAY),
                    listing("B", 500_000, 3, "Reston", TODAY),
                    listing("C", 500_000, 3, "Reston", TODAY)));
            assertEquals(ids(svc.search(SearchCriteria.empty())), ids(svc.search(SearchCriteria.empty())));
        }

        @Test
        void resultCarriesScoreBreakdown() {
            SearchService svc = service(List.of(listing("A", 500_000, 3, "Reston", TODAY)));
            SearchResult.Item item = svc.search(criteria().targetBudget(500_000).build()).items().get(0);
            assertEquals(100.0, item.score());
            assertEquals(1.0, item.scoreBreakdown().budget());
            assertEquals(1.0, item.scoreBreakdown().recency());
        }
    }

    @Nested
    class Pagination {

        /** Seven listings, strictly ordered newest-first: P0 (today) .. P6 (6 days ago). */
        private final SearchService svc = service(java.util.stream.IntStream.range(0, 7)
                .mapToObj(i -> listing("P" + i, 500_000, 3, "Reston", TODAY.minusDays(i)))
                .toList());

        @Test
        void defaultsToFirstPageOfFive() {
            SearchResult r = svc.search(SearchCriteria.empty());
            assertEquals(1, r.page());
            assertEquals(5, r.pageSize());
            assertEquals(7, r.totalItems());
            assertEquals(2, r.totalPages());
            assertEquals(List.of("P0", "P1", "P2", "P3", "P4"), ids(r));
        }

        @Test
        void lastPageIsPartial() {
            assertEquals(List.of("P5", "P6"), ids(svc.search(criteria().page(2).build())));
        }

        @Test
        void pagesDoNotOverlapAndCoverEverything() {
            List<String> all = new java.util.ArrayList<>();
            for (int p = 1; p <= 4; p++) {
                all.addAll(ids(svc.search(criteria().page(p).pageSize(2).build())));
            }
            assertEquals(List.of("P0", "P1", "P2", "P3", "P4", "P5", "P6"), all);
        }

        @Test
        void exactMultipleHasNoEmptyTrailingPage() {
            SearchResult r = svc.search(criteria().pageSize(7).build());
            assertEquals(1, r.totalPages());
            assertThrows(InvalidSearchException.class, () -> svc.search(criteria().pageSize(7).page(2).build()));
        }

        @Test
        void pageSizeLargerThanTotalReturnsEverythingOnOnePage() {
            SearchResult r = svc.search(criteria().pageSize(50).build());
            assertEquals(7, r.items().size());
            assertEquals(1, r.totalPages());
        }

        @Test
        void pageBeyondLastPageIsAClearError() {
            InvalidSearchException e = assertThrows(InvalidSearchException.class,
                    () -> svc.search(criteria().page(3).build()));
            assertEquals("page", e.getErrors().get(0).field());
            assertTrue(e.getMessage().contains("out of range"));
        }

        @Test
        void firstPageOfEmptyResultIsFine() {
            SearchResult r = svc.search(criteria().city("Nowhere").build());
            assertEquals(0, r.totalPages());
            assertTrue(r.items().isEmpty());
        }

        @Test
        void pageSizeOfOneWalksEveryItem() {
            SearchResult last = svc.search(criteria().pageSize(1).page(7).build());
            assertEquals(List.of("P6"), ids(last));
            assertEquals(7, last.totalPages());
        }
    }

    @Nested
    class Duplicates {

        @Test
        void itemsListTheirPossibleDuplicates() {
            Listing a = listing("A1", "MLS_A", "123 Main St, Apt 4B", "Springfield", 450_000, 2, TODAY, "active", "");
            Listing b = listing("B7", "MLS_B", "123 Main Street, Unit 4B", "Springfield", 452_000, 2, TODAY, "active", "");
            Listing c = listing("A2", "MLS_A", "456 Oak Ave", "Springfield", 525_000, 3, TODAY, "active", "");
            SearchService svc = service(List.of(a, b, c));

            SearchResult r = svc.search(criteria().pageSize(10).build());
            SearchResult.Item itemA = r.items().stream().filter(i -> i.id().equals("A1")).findFirst().orElseThrow();
            SearchResult.Item itemC = r.items().stream().filter(i -> i.id().equals("A2")).findFirst().orElseThrow();

            assertEquals(1, itemA.possibleDuplicates().size());
            assertEquals("B7", itemA.possibleDuplicates().get(0).id());
            assertEquals("MLS_B", itemA.possibleDuplicates().get(0).source());
            assertTrue(itemC.possibleDuplicates().isEmpty());
        }

        @Test
        void duplicatesAreFlaggedEvenWhenTheOtherCopyIsFilteredOut() {
            Listing a = listing("A1", "MLS_A", "123 Main St", "Springfield", 450_000, 2, TODAY, "active", "");
            Listing b = listing("B7", "MLS_B", "123 Main Street", "Springfield", 470_000, 2, TODAY, "active", "");
            SearchService svc = service(List.of(a, b));
            SearchResult r = svc.search(criteria().maxPrice(460_000).build());
            assertEquals(List.of("A1"), ids(r));
            assertEquals("B7", r.items().get(0).possibleDuplicates().get(0).id());
        }
    }
}
