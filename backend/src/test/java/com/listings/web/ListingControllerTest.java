package com.listings.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end through HTTP against the real sample_listings.json:
 * request binding, JSON shape, and error mapping.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ListingControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void returnsFirstPageOfRankedResultsWithMetadata() throws Exception {
        mvc.perform(get("/api/listings").param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(5)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(5))
                .andExpect(jsonPath("$.totalItems").value(12))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.items[0].address").exists())
                .andExpect(jsonPath("$.items[0].price").exists())
                .andExpect(jsonPath("$.items[0].bedrooms").exists())
                .andExpect(jsonPath("$.items[0].score").exists())
                .andExpect(jsonPath("$.items[0].listedDate").value(org.hamcrest.Matchers.matchesPattern("\\d{4}-\\d{2}-\\d{2}")));
    }

    @Test
    void appliesFiltersFromQueryString() throws Exception {
        mvc.perform(get("/api/listings")
                        .param("city", "springfield")
                        .param("minBedrooms", "3")
                        .param("keyword", "kitchen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void flagsCrossFeedDuplicatesInSampleData() throws Exception {
        mvc.perform(get("/api/listings").param("keyword", "top floor condo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].possibleDuplicates", hasSize(1)));
    }

    @Test
    void minPriceAboveMaxPriceIs400WithFieldDetails() throws Exception {
        mvc.perform(get("/api/listings").param("minPrice", "600000").param("maxPrice", "400000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details[0].field").value("minPrice"))
                .andExpect(jsonPath("$.message", containsString("cannot be greater than maxPrice")));
    }

    @Test
    void zeroPageSizeIs400() throws Exception {
        mvc.perform(get("/api/listings").param("pageSize", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("pageSize"));
    }

    @Test
    void nonNumericParameterIs400NotA500() throws Exception {
        mvc.perform(get("/api/listings").param("minPrice", "cheap"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("minPrice"))
                .andExpect(jsonPath("$.message", containsString("whole number")));
    }

    @Test
    void pageOutOfRangeIs400() throws Exception {
        mvc.perform(get("/api/listings").param("page", "99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("page"));
    }

    @Test
    void unknownCityIs200WithEmptyItemsAndNotice() throws Exception {
        mvc.perform(get("/api/listings").param("city", "Gotham"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.notices[0]", containsString("Gotham")));
    }

    @Test
    void citiesEndpointListsDistinctCities() throws Exception {
        mvc.perform(get("/api/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(6))));
    }
}
