package com.listings.search.controller;

import static com.listings.search.support.TestListings.listing;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.listings.search.config.SearchProperties;
import com.listings.search.exception.PageOutOfRangeException;
import com.listings.search.mapper.ListingMapper;
import com.listings.search.model.ListingStatus;
import com.listings.search.model.RelevanceScore;
import com.listings.search.model.ScoredListing;
import com.listings.search.model.SearchCriteria;
import com.listings.search.model.SearchResult;
import com.listings.search.service.ListingSearchService;

/**
 * Web-layer slice test: request binding, Bean Validation, JSON shape and error mapping.
 * The service is mocked so these tests are only about HTTP behaviour.
 */
@WebMvcTest(ListingController.class)
@Import(ListingControllerTest.MapperConfig.class)
class ListingControllerTest {

    @TestConfiguration
    static class MapperConfig {
        @Bean
        ListingMapper listingMapper() {
            return new ListingMapper(SearchProperties.defaults());
        }
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ListingSearchService searchService;

    private static SearchResult oneResult() {
        ScoredListing item = new ScoredListing(
                listing("A1").address("123 Main St").price(450_000).bedrooms(2).build(),
                new RelevanceScore(91.2, 0.9, 0.95),
                List.of(listing("B7").source("MLS_B").address("123 Main Street").price(452_000).build()));
        return new SearchResult(List.of(item), 1, 5, 1, 1, List.of());
    }

    @Test
    void returnsRankedPageWithScoreAndDuplicates() throws Exception {
        given(searchService.search(any())).willReturn(oneResult());

        mvc.perform(get("/api/v1/listings"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].address").value("123 Main St"))
                .andExpect(jsonPath("$.items[0].price").value(450000))
                .andExpect(jsonPath("$.items[0].bedrooms").value(2))
                .andExpect(jsonPath("$.items[0].status").value("active"))
                .andExpect(jsonPath("$.items[0].listedDate").value("2026-09-05"))
                .andExpect(jsonPath("$.items[0].score").value(91.2))
                .andExpect(jsonPath("$.items[0].scoreBreakdown.budget").value(0.9))
                .andExpect(jsonPath("$.items[0].possibleDuplicates[0].source").value("MLS_B"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void bindsAndNormalizesQueryParameters() throws Exception {
        given(searchService.search(any())).willReturn(oneResult());

        mvc.perform(get("/api/v1/listings")
                        .param("minPrice", "400000")
                        .param("maxPrice", "600000")
                        .param("minBedrooms", "3")
                        .param("city", "  Reston ")
                        .param("keyword", "pool")
                        .param("targetBudget", "500000")
                        .param("status", "PENDING")
                        .param("page", "2")
                        .param("pageSize", "10"))
                .andExpect(status().isOk());

        ArgumentCaptor<SearchCriteria> captor = ArgumentCaptor.forClass(SearchCriteria.class);
        then(searchService).should().search(captor.capture());
        SearchCriteria criteria = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(criteria).isEqualTo(new SearchCriteria(
                new BigDecimal("400000"), new BigDecimal("600000"), 3, "Reston", "pool",
                new BigDecimal("500000"), ListingStatus.PENDING, 2, 10));
    }

    @Test
    void minPriceAboveMaxPriceIsProblemDetail400() throws Exception {
        mvc.perform(get("/api/v1/listings").param("minPrice", "600000").param("maxPrice", "400000"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Invalid request parameters"))
                .andExpect(jsonPath("$.detail").value("minPrice cannot be greater than maxPrice"))
                .andExpect(jsonPath("$.errors[0].field").value("minPrice"));

        then(searchService).should(never()).search(any());
    }

    @Test
    void zeroPageSizeIs400() throws Exception {
        mvc.perform(get("/api/v1/listings").param("pageSize", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("pageSize"))
                .andExpect(jsonPath("$.errors[0].message").value("pageSize must be 1 or greater"));
    }

    @Test
    void nonNumericValueIs400WithReadableMessageNot500() throws Exception {
        mvc.perform(get("/api/v1/listings").param("minPrice", "cheap"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("minPrice"))
                .andExpect(jsonPath("$.errors[0].message").value("minPrice must be a number (got 'cheap')"));
    }

    @Test
    void reportsEveryInvalidFieldAtOnce() throws Exception {
        mvc.perform(get("/api/v1/listings")
                        .param("page", "0")
                        .param("pageSize", "999")
                        .param("status", "foreclosed"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("3 invalid search parameters"))
                .andExpect(jsonPath("$.errors", hasSize(3)));
    }

    @Test
    void pageOutOfRangeIs400OnPageField() throws Exception {
        given(searchService.search(any())).willThrow(new PageOutOfRangeException(9, 2));

        mvc.perform(get("/api/v1/listings").param("page", "9"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("page"))
                .andExpect(jsonPath("$.detail", containsString("out of range")));
    }

    @Test
    void unexpectedErrorsAre500WithoutLeakingInternals() throws Exception {
        given(searchService.search(any())).willThrow(new IllegalStateException("db password is hunter2"));

        mvc.perform(get("/api/v1/listings"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Something went wrong. Please try again."));
    }

    @Test
    void citiesEndpoint() throws Exception {
        given(searchService.findCities()).willReturn(List.of("Reston", "Vienna"));

        mvc.perform(get("/api/v1/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0]").value("Reston"));
    }
}
