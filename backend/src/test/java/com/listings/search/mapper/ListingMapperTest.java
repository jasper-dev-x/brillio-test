package com.listings.search.mapper;

import static com.listings.search.support.TestListings.listing;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.listings.search.config.SearchProperties;
import com.listings.search.dto.ListingResponse;
import com.listings.search.dto.ListingSearchRequest;
import com.listings.search.dto.SearchResponse;
import com.listings.search.model.Listing;
import com.listings.search.model.ListingStatus;
import com.listings.search.model.RelevanceScore;
import com.listings.search.model.ScoredListing;
import com.listings.search.model.SearchCriteria;
import com.listings.search.model.SearchResult;

class ListingMapperTest {

    private final ListingMapper mapper = new ListingMapper(new SearchProperties(7));

    @Test
    void appliesDefaultsForMissingPagingValues() {
        SearchCriteria criteria = mapper.toCriteria(new ListingSearchRequest());

        assertThat(criteria.page()).isEqualTo(1);
        assertThat(criteria.pageSize()).isEqualTo(7);
    }

    @Test
    void normalizesTextAndStatus() {
        ListingSearchRequest request = new ListingSearchRequest();
        request.setCity("  Reston ");
        request.setKeyword("   ");
        request.setStatus(" Pending ");
        request.setMinPrice(BigDecimal.valueOf(100));

        SearchCriteria criteria = mapper.toCriteria(request);

        assertThat(criteria.city()).isEqualTo("Reston");
        assertThat(criteria.keyword()).isNull();
        assertThat(criteria.status()).isEqualTo(ListingStatus.PENDING);
        assertThat(criteria.minPrice()).isEqualTo(BigDecimal.valueOf(100));
    }

    @Test
    void mapsScoredListingToResponse() {
        Listing duplicate = listing("B7").source("MLS_B").address("123 Main Street").price(452_000).build();
        Listing original = listing("A1").address("123 Main St").price(450_000).build();
        SearchResult result = new SearchResult(
                List.of(new ScoredListing(original, new RelevanceScore(87.5, 0.9, 0.8), List.of(duplicate))),
                1, 5, 1, 1, List.of("note"));

        SearchResponse response = mapper.toResponse(result);

        assertThat(response.totalItems()).isEqualTo(1);
        assertThat(response.notices()).containsExactly("note");
        ListingResponse item = response.items().get(0);
        assertThat(item.id()).isEqualTo("A1");
        assertThat(item.status()).isEqualTo("active");
        assertThat(item.score()).isEqualTo(87.5);
        assertThat(item.scoreBreakdown().budget()).isEqualTo(0.9);
        assertThat(item.possibleDuplicates()).hasSize(1);
        assertThat(item.possibleDuplicates().get(0).id()).isEqualTo("B7");
        assertThat(item.possibleDuplicates().get(0).source()).isEqualTo("MLS_B");
    }
}
