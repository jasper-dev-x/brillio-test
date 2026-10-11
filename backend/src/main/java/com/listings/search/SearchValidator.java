package com.listings.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * Validates search input up front so bad values produce a clear 400 instead of
 * a crash, a silently empty result, or wrong data. All problems are collected
 * and reported together so the UI can highlight every bad field at once.
 *
 * Page-out-of-range is NOT checked here: it depends on how many results match,
 * so {@link SearchService} checks it after filtering.
 */
@Component
public class SearchValidator {

    static final Set<String> VALID_STATUSES = Set.of("active", "pending", "sold");
    static final int MAX_TEXT_LENGTH = 100;

    public void validate(SearchCriteria c) {
        List<FieldError> errors = new ArrayList<>();

        if (c.minPrice() != null && c.minPrice() < 0) {
            errors.add(new FieldError("minPrice", "minPrice must be 0 or greater"));
        }
        if (c.maxPrice() != null && c.maxPrice() < 0) {
            errors.add(new FieldError("maxPrice", "maxPrice must be 0 or greater"));
        }
        if (c.minPrice() != null && c.maxPrice() != null && c.minPrice() > c.maxPrice()) {
            errors.add(new FieldError("minPrice",
                    "minPrice (" + c.minPrice() + ") cannot be greater than maxPrice (" + c.maxPrice() + ")"));
        }
        if (c.minBedrooms() != null && c.minBedrooms() < 0) {
            errors.add(new FieldError("minBedrooms", "minBedrooms must be 0 or greater"));
        }
        if (c.targetBudget() != null && c.targetBudget() <= 0) {
            errors.add(new FieldError("targetBudget", "targetBudget must be greater than 0"));
        }
        if (c.page() != null && c.page() < 1) {
            errors.add(new FieldError("page", "page must be 1 or greater"));
        }
        if (c.pageSize() != null && c.pageSize() < 1) {
            errors.add(new FieldError("pageSize", "pageSize must be 1 or greater"));
        } else if (c.pageSize() != null && c.pageSize() > SearchCriteria.MAX_PAGE_SIZE) {
            errors.add(new FieldError("pageSize", "pageSize cannot exceed " + SearchCriteria.MAX_PAGE_SIZE));
        }
        String status = c.normalizedStatus();
        if (status != null && !VALID_STATUSES.contains(status)) {
            errors.add(new FieldError("status", "status must be one of active, pending, sold"));
        }
        checkLength(errors, "city", c.city());
        checkLength(errors, "keyword", c.keyword());

        if (!errors.isEmpty()) {
            throw new InvalidSearchException(errors);
        }
    }

    private static void checkLength(List<FieldError> errors, String field, String value) {
        if (value != null && value.length() > MAX_TEXT_LENGTH) {
            errors.add(new FieldError(field, field + " cannot be longer than " + MAX_TEXT_LENGTH + " characters"));
        }
    }
}
