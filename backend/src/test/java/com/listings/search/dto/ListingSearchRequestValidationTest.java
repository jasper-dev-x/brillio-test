package com.listings.search.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/** Exercises the Bean Validation rules on the request DTO directly (no Spring context). */
class ListingSearchRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static ListingSearchRequest request(Consumer<ListingSearchRequest> customizer) {
        ListingSearchRequest request = new ListingSearchRequest();
        customizer.accept(request);
        return request;
    }

    private static List<String> invalidFields(ListingSearchRequest request) {
        Set<ConstraintViolation<ListingSearchRequest>> violations = validator.validate(request);
        return violations.stream().map(v -> v.getPropertyPath().toString()).sorted().toList();
    }

    @Test
    void emptyRequestIsValid() {
        assertThat(invalidFields(new ListingSearchRequest())).isEmpty();
    }

    @Test
    void fullyPopulatedValidRequestPasses() {
        assertThat(invalidFields(request(r -> {
            r.setMinPrice(BigDecimal.valueOf(100_000));
            r.setMaxPrice(BigDecimal.valueOf(600_000));
            r.setMinBedrooms(2);
            r.setCity("Reston");
            r.setKeyword("pool");
            r.setTargetBudget(BigDecimal.valueOf(500_000));
            r.setStatus("Active");
            r.setPage(2);
            r.setPageSize(50);
        }))).isEmpty();
    }

    @Test
    void minPriceGreaterThanMaxPriceIsReportedOnMinPrice() {
        ListingSearchRequest request = request(r -> {
            r.setMinPrice(BigDecimal.valueOf(600_000));
            r.setMaxPrice(BigDecimal.valueOf(400_000));
        });

        Set<ConstraintViolation<ListingSearchRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        ConstraintViolation<ListingSearchRequest> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("minPrice");
        assertThat(violation.getMessage()).isEqualTo("minPrice cannot be greater than maxPrice");
    }

    @Test
    void equalMinAndMaxPriceIsAllowed() {
        assertThat(invalidFields(request(r -> {
            r.setMinPrice(BigDecimal.valueOf(450_000));
            r.setMaxPrice(BigDecimal.valueOf(450_000));
        }))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, ListingSearchRequest.MAX_PAGE_SIZE + 1})
    void pageSizeOutOfBoundsIsRejected(int pageSize) {
        assertThat(invalidFields(request(r -> r.setPageSize(pageSize)))).containsExactly("pageSize");
    }

    @Test
    void pageBelowOneIsRejected() {
        assertThat(invalidFields(request(r -> r.setPage(0)))).containsExactly("page");
    }

    @Test
    void negativeValuesAreRejected() {
        assertThat(invalidFields(request(r -> {
            r.setMinPrice(BigDecimal.valueOf(-1));
            r.setMaxPrice(BigDecimal.valueOf(-1));
            r.setMinBedrooms(-1);
        }))).containsExactly("maxPrice", "minBedrooms", "minPrice");
    }

    @Test
    void zeroTargetBudgetIsRejected() {
        assertThat(invalidFields(request(r -> r.setTargetBudget(BigDecimal.ZERO)))).containsExactly("targetBudget");
    }

    @Test
    void unknownStatusIsRejectedButCaseIsIgnored() {
        assertThat(invalidFields(request(r -> r.setStatus("foreclosed")))).containsExactly("status");
        assertThat(invalidFields(request(r -> r.setStatus("SOLD")))).isEmpty();
    }

    @Test
    void overlyLongTextIsRejected() {
        assertThat(invalidFields(request(r -> r.setKeyword("x".repeat(101))))).containsExactly("keyword");
    }

    @Test
    void everyProblemIsReportedTogether() {
        assertThat(invalidFields(request(r -> {
            r.setMinPrice(BigDecimal.TEN);
            r.setMaxPrice(BigDecimal.ONE);
            r.setPage(0);
            r.setPageSize(0);
        }))).containsExactly("minPrice", "page", "pageSize");
    }
}
