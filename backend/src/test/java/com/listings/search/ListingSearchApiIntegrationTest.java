package com.listings.search;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Full application context against the real sample_listings.json, with "today" pinned so
 * ranking assertions are stable.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ListingSearchApiIntegrationTest {

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(LocalDate.of(2026, 9, 5).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mvc;

    @Test
    void defaultSearchReturnsFirstPageOfAllTwelveListings() throws Exception {
        mvc.perform(get("/api/v1/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(5)))
                .andExpect(jsonPath("$.totalItems").value(12))
                .andExpect(jsonPath("$.totalPages").value(3))
                // Without a budget, newest first: A5 was listed 2026-09-04
                .andExpect(jsonPath("$.items[0].id").value("A5"));
    }

    @Test
    void budgetRankingOnRealData() throws Exception {
        // In Springfield with a 450k target, A1 (450k, exact) beats its 452k MLS_B twin.
        mvc.perform(get("/api/v1/listings").param("city", "springfield").param("targetBudget", "450000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(4))
                .andExpect(jsonPath("$.items[0].id").value("A1"))
                .andExpect(jsonPath("$.items[0].possibleDuplicates[0].id").value("B7"));
    }

    @Test
    void keywordAndBedroomsFilterOnRealData() throws Exception {
        mvc.perform(get("/api/v1/listings")
                        .param("city", "Springfield").param("minBedrooms", "3").param("keyword", "kitchen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void unknownCityIs200WithNotice() throws Exception {
        mvc.perform(get("/api/v1/listings").param("city", "Gotham"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.notices[0]", containsString("Gotham")));
    }

    @Test
    void pageOutOfRangeIs400() throws Exception {
        mvc.perform(get("/api/v1/listings").param("page", "99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("page"));
    }

    @Test
    void unknownPathUsesProblemDetailToo() throws Exception {
        mvc.perform(get("/api/v1/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void healthAndOpenApiAreExposed() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/listings']").exists());
    }
}
