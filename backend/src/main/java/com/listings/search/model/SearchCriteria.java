package com.listings.search.model;

import java.math.BigDecimal;

/**
 * Validated, normalized search input handed to the service layer.
 * Text fields are trimmed (blank becomes null); paging values are always set.
 */
public record SearchCriteria(
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer minBedrooms,
        String city,
        String keyword,
        BigDecimal targetBudget,
        ListingStatus status,
        int page,
        int pageSize) {

    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder; mainly for tests and callers that only set a few fields. */
    public static final class Builder {
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private Integer minBedrooms;
        private String city;
        private String keyword;
        private BigDecimal targetBudget;
        private ListingStatus status;
        private int page = 1;
        private int pageSize = 5;

        public Builder minPrice(long v) { this.minPrice = BigDecimal.valueOf(v); return this; }
        public Builder maxPrice(long v) { this.maxPrice = BigDecimal.valueOf(v); return this; }
        public Builder minBedrooms(int v) { this.minBedrooms = v; return this; }
        public Builder city(String v) { this.city = v; return this; }
        public Builder keyword(String v) { this.keyword = v; return this; }
        public Builder targetBudget(long v) { this.targetBudget = BigDecimal.valueOf(v); return this; }
        public Builder status(ListingStatus v) { this.status = v; return this; }
        public Builder page(int v) { this.page = v; return this; }
        public Builder pageSize(int v) { this.pageSize = v; return this; }

        public SearchCriteria build() {
            return new SearchCriteria(minPrice, maxPrice, minBedrooms, blankToNull(city), blankToNull(keyword),
                    targetBudget, status, page, pageSize);
        }
    }

    public static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
