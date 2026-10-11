package com.listings.search.service;

import static com.listings.search.support.TestListings.listing;
import static com.listings.search.support.TestListings.service;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.listings.search.exception.PageOutOfRangeException;
import com.listings.search.model.Listing;
import com.listings.search.model.ListingStatus;
import com.listings.search.model.ScoredListing;
import com.listings.search.model.SearchCriteria;
import com.listings.search.model.SearchResult;

class DefaultListingSearchServiceTest {

    private static List<String> ids(SearchResult result) {
        return result.items().stream().map(s -> s.listing().id()).toList();
    }

    private static List<String> sortedIds(SearchResult result) {
        return ids(result).stream().sorted().toList();
    }

    private static SearchCriteria.Builder criteria() {
        return SearchCriteria.builder().pageSize(10);
    }

    @Nested
    @DisplayName("filtering")
    class Filtering {

        private final DefaultListingSearchService service = service(List.of(
                listing("L1").city("Reston").price(400_000).bedrooms(2).description("Pet friendly condo with pool").build(),
                listing("L2").city("Reston").price(500_000).bedrooms(3).status(ListingStatus.PENDING)
                        .description("Townhome, community POOL").build(),
                listing("L3").source("MLS_B").city("Vienna").price(600_000).bedrooms(4).description("Large yard, no pets").build(),
                listing("L4").source("MLS_B").city("Fairfax").price(700_000).bedrooms(5).status(ListingStatus.SOLD)
                        .description("Garage and pool").build()));

        @Test
        void noFiltersReturnsEverything() {
            assertThat(service.search(criteria().build()).totalItems()).isEqualTo(4);
        }

        @Test
        void priceBoundsAreInclusive() {
            SearchResult result = service.search(criteria().minPrice(500_000).maxPrice(600_000).build());
            assertThat(sortedIds(result)).containsExactly("L2", "L3");
        }

        @Test
        void minBedroomsIsInclusive() {
            assertThat(sortedIds(service.search(criteria().minBedrooms(4).build()))).containsExactly("L3", "L4");
        }

        @Test
        void cityIsCaseInsensitiveAndTrimmed() {
            SearchResult result = service.search(criteria().city("  reSTON ").build());
            assertThat(result.totalItems()).isEqualTo(2);
            assertThat(result.notices()).isEmpty();
        }

        @Test
        void keywordIsCaseInsensitiveAndRequiresEveryTerm() {
            assertThat(service.search(criteria().keyword("Pool").build()).totalItems()).isEqualTo(3);
            assertThat(ids(service.search(criteria().keyword("pool  PET").build()))).containsExactly("L1");
        }

        @Test
        void statusFilter() {
            assertThat(ids(service.search(criteria().status(ListingStatus.SOLD).build()))).containsExactly("L4");
        }

        @Test
        void filtersCombineWithAndSemantics() {
            SearchResult result = service.search(criteria().city("Reston").keyword("pool").minBedrooms(3).build());
            assertThat(ids(result)).containsExactly("L2");
        }

        @Test
        void noMatchesIsAnEmptyPageNotAnError() {
            SearchResult result = service.search(criteria().minPrice(10_000_000).build());

            assertThat(result.items()).isEmpty();
            assertThat(result.totalItems()).isEqualTo(0);
            assertThat(result.totalPages()).isEqualTo(0);
            assertThat(result.notices()).isEmpty();
        }

        @Test
        void unknownCityReturnsEmptyWithExplanatoryNotice() {
            SearchResult result = service.search(criteria().city("Atlantis").build());

            assertThat(result.items()).isEmpty();
            assertThat(result.notices()).hasSize(1);
            assertThat(result.notices().get(0)).contains("Atlantis").contains("Reston");
        }

        @Test
        void knownCityFilteredOutByOtherCriteriaHasNoCityNotice() {
            SearchResult result = service.search(criteria().city("Vienna").minPrice(9_000_000).build());

            assertThat(result.items()).isEmpty();
            assertThat(result.notices()).isEmpty();
        }

        @Test
        void citiesAreDistinctAndSorted() {
            assertThat(service.findCities()).containsExactly("Fairfax", "Reston", "Vienna");
        }
    }

    @Nested
    @DisplayName("ranking")
    class Ranking {

        @Test
        void closerToBudgetRanksHigherWhenEquallyRecent() {
            DefaultListingSearchService service = service(List.of(
                    listing("FAR").price(300_000).build(),
                    listing("EXACT").price(500_000).build(),
                    listing("NEAR").price(520_000).build()));

            assertThat(ids(service.search(criteria().targetBudget(500_000).build())))
                    .containsExactly("EXACT", "NEAR", "FAR");
        }

        @Test
        void newerRanksHigherWithoutBudget() {
            DefaultListingSearchService service = service(List.of(
                    listing("OLD").daysAgo(60).build(),
                    listing("NEW").daysAgo(0).build(),
                    listing("MID").daysAgo(10).build()));

            assertThat(ids(service.search(criteria().build()))).containsExactly("NEW", "MID", "OLD");
        }

        @Test
        void tiedScoresOnSameDateBreakByLowerPrice() {
            // Both far over budget (0 budget credit) and listed the same day -> identical scores.
            DefaultListingSearchService service = service(List.of(
                    listing("PRICEY").price(900_000).build(),
                    listing("CHEAP").price(800_000).build()));

            SearchResult result = service.search(criteria().targetBudget(100_000).build());

            assertThat(result.items().get(0).score().total()).isEqualTo(result.items().get(1).score().total());
            assertThat(ids(result)).containsExactly("CHEAP", "PRICEY");
        }

        @Test
        void tiedScoresPreferNewerListingOverLowerPrice() {
            // Over a year old, both recency scores round to 0.01 -> tie; the newer one must win.
            DefaultListingSearchService service = service(List.of(
                    listing("OLDER_CHEAP").price(400_000).daysAgo(401).build(),
                    listing("OLD_PRICEY").price(600_000).daysAgo(400).build()));

            SearchResult result = service.search(criteria().build());

            assertThat(result.items().get(0).score().total()).isEqualTo(result.items().get(1).score().total());
            assertThat(ids(result)).containsExactly("OLD_PRICEY", "OLDER_CHEAP");
        }

        @Test
        void fullyIdenticalListingsBreakTiesBySourceThenId() {
            DefaultListingSearchService service = service(List.of(
                    listing("B").source("MLS_B").build(),
                    listing("Z").source("MLS_A").build(),
                    listing("A").source("MLS_A").build()));

            assertThat(ids(service.search(criteria().build()))).containsExactly("A", "Z", "B");
        }

        @Test
        void scoreBreakdownIsExposed() {
            DefaultListingSearchService service = service(List.of(listing("A").price(500_000).build()));

            ScoredListing item = service.search(criteria().targetBudget(500_000).build()).items().get(0);

            assertThat(item.score().total()).isEqualTo(100.0);
            assertThat(item.score().budget()).isEqualTo(1.0);
            assertThat(item.score().recency()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("pagination")
    class Pagination {

        /** Seven listings strictly ordered newest-first: P0 (today) .. P6 (6 days ago). */
        private final DefaultListingSearchService service = service(IntStream.range(0, 7)
                .mapToObj(i -> listing("P" + i).daysAgo(i).build())
                .toList());

        private SearchResult page(int page, int pageSize) {
            return service.search(SearchCriteria.builder().page(page).pageSize(pageSize).build());
        }

        @Test
        void firstPageCarriesMetadata() {
            SearchResult result = page(1, 5);

            assertThat(ids(result)).containsExactly("P0", "P1", "P2", "P3", "P4");
            assertThat(result.page()).isEqualTo(1);
            assertThat(result.pageSize()).isEqualTo(5);
            assertThat(result.totalItems()).isEqualTo(7);
            assertThat(result.totalPages()).isEqualTo(2);
        }

        @Test
        void lastPageIsPartial() {
            assertThat(ids(page(2, 5))).containsExactly("P5", "P6");
        }

        @Test
        void pagesNeverOverlapAndCoverEveryItem() {
            List<String> all = new ArrayList<>();
            for (int p = 1; p <= 4; p++) {
                all.addAll(ids(page(p, 2)));
            }
            assertThat(all).containsExactly("P0", "P1", "P2", "P3", "P4", "P5", "P6");
        }

        @Test
        void exactMultipleHasNoTrailingEmptyPage() {
            assertThat(page(1, 7).totalPages()).isEqualTo(1);
            assertThatThrownBy(() -> page(2, 7)).isInstanceOf(PageOutOfRangeException.class);
        }

        @Test
        void pageSizeLargerThanTotalReturnsEverything() {
            SearchResult result = page(1, 50);
            assertThat(result.items()).hasSize(7);
            assertThat(result.totalPages()).isEqualTo(1);
        }

        @Test
        void pageSizeOfOneReachesTheLastItem() {
            SearchResult result = page(7, 1);
            assertThat(ids(result)).containsExactly("P6");
            assertThat(result.totalPages()).isEqualTo(7);
        }

        @Test
        void pageBeyondTheLastPageIsRejected() {
            assertThatThrownBy(() -> page(3, 5))
                    .isInstanceOf(PageOutOfRangeException.class)
                    .hasMessageContaining("out of range");
        }

        @Test
        void firstPageOfAnEmptyResultIsFine() {
            SearchResult result = service.search(SearchCriteria.builder().city("Nowhere").build());
            assertThat(result.items()).isEmpty();
            assertThat(result.totalPages()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("duplicates")
    class Duplicates {

        private final Listing a = listing("A1").source("MLS_A").address("123 Main St, Apt 4B").price(450_000).build();
        private final Listing b = listing("B7").source("MLS_B").address("123 Main Street, Unit 4B").price(470_000).build();
        private final Listing c = listing("A2").source("MLS_A").address("456 Oak Ave").build();

        @Test
        void itemsListTheirLikelyDuplicates() {
            SearchResult result = service(List.of(a, b, c)).search(criteria().build());

            ScoredListing itemA = result.items().stream().filter(i -> i.listing().id().equals("A1")).findFirst().orElseThrow();
            ScoredListing itemC = result.items().stream().filter(i -> i.listing().id().equals("A2")).findFirst().orElseThrow();

            assertThat(itemA.possibleDuplicates()).containsExactly(b);
            assertThat(itemC.possibleDuplicates()).isEmpty();
        }

        @Test
        void flaggedEvenWhenTheOtherCopyIsFilteredOut() {
            SearchResult result = service(List.of(a, b)).search(criteria().maxPrice(460_000).build());

            assertThat(ids(result)).containsExactly("A1");
            assertThat(result.items().get(0).possibleDuplicates()).containsExactly(b);
        }
    }
}
