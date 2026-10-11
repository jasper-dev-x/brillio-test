package com.listings.search;

import static com.listings.search.TestListings.criteria;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class SearchValidatorTest {

    private final SearchValidator validator = new SearchValidator();

    private List<String> invalidFields(SearchCriteria c) {
        InvalidSearchException e = assertThrows(InvalidSearchException.class, () -> validator.validate(c));
        return e.getErrors().stream().map(FieldError::field).toList();
    }

    @Test
    void emptyCriteriaIsValid() {
        assertDoesNotThrow(() -> validator.validate(SearchCriteria.empty()));
    }

    @Test
    void fullyPopulatedValidCriteriaPasses() {
        assertDoesNotThrow(() -> validator.validate(criteria()
                .minPrice(100_000).maxPrice(600_000).minBedrooms(2).city("Reston").keyword("pool")
                .targetBudget(500_000).status("Active").page(2).pageSize(50).build()));
    }

    @Test
    void minPriceGreaterThanMaxPriceIsRejected() {
        InvalidSearchException e = assertThrows(InvalidSearchException.class,
                () -> validator.validate(criteria().minPrice(600_000).maxPrice(400_000).build()));
        assertEquals("minPrice", e.getErrors().get(0).field());
        assertTrue(e.getMessage().contains("cannot be greater than maxPrice"));
    }

    @Test
    void minPriceEqualToMaxPriceIsAllowed() {
        assertDoesNotThrow(() -> validator.validate(criteria().minPrice(450_000).maxPrice(450_000).build()));
    }

    @Test
    void zeroOrNegativePageSizeIsRejected() {
        assertEquals(List.of("pageSize"), invalidFields(criteria().pageSize(0).build()));
        assertEquals(List.of("pageSize"), invalidFields(criteria().pageSize(-5).build()));
    }

    @Test
    void pageSizeAboveMaximumIsRejected() {
        assertEquals(List.of("pageSize"), invalidFields(criteria().pageSize(SearchCriteria.MAX_PAGE_SIZE + 1).build()));
    }

    @Test
    void pageBelowOneIsRejected() {
        assertEquals(List.of("page"), invalidFields(criteria().page(0).build()));
    }

    @Test
    void negativeNumbersAreRejected() {
        assertEquals(List.of("minPrice"), invalidFields(criteria().minPrice(-1).build()));
        assertEquals(List.of("maxPrice"), invalidFields(criteria().maxPrice(-1).build()));
        assertEquals(List.of("minBedrooms"), invalidFields(criteria().minBedrooms(-1).build()));
    }

    @Test
    void nonPositiveTargetBudgetIsRejected() {
        assertEquals(List.of("targetBudget"), invalidFields(criteria().targetBudget(0).build()));
    }

    @Test
    void unknownStatusIsRejected() {
        assertEquals(List.of("status"), invalidFields(criteria().status("foreclosed").build()));
    }

    @Test
    void overlyLongTextIsRejected() {
        assertEquals(List.of("keyword"), invalidFields(criteria().keyword("x".repeat(101)).build()));
    }

    @Test
    void allProblemsAreReportedTogether() {
        List<String> fields = invalidFields(criteria().minPrice(9).maxPrice(1).pageSize(0).page(0).build());
        assertEquals(List.of("minPrice", "page", "pageSize"), fields);
    }
}
