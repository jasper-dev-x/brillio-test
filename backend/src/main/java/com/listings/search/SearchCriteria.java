package com.listings.search;

/**
 * Raw, user-supplied search parameters. Every field is nullable ("not provided").
 * Values are not trusted until {@link SearchValidator#validate(SearchCriteria)} has run.
 */
public record SearchCriteria(
        Long minPrice,
        Long maxPrice,
        Integer minBedrooms,
        String city,
        String keyword,
        Long targetBudget,
        String status,
        Integer page,
        Integer pageSize) {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 5;
    public static final int MAX_PAGE_SIZE = 50;

    public int pageOrDefault() {
        return page == null ? DEFAULT_PAGE : page;
    }

    public int pageSizeOrDefault() {
        return pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
    }

    /** Trimmed city, or null if blank. */
    public String normalizedCity() {
        return blankToNull(city);
    }

    /** Trimmed keyword, or null if blank. */
    public String normalizedKeyword() {
        return blankToNull(keyword);
    }

    /** Trimmed, lower-cased status, or null if blank. */
    public String normalizedStatus() {
        String s = blankToNull(status);
        return s == null ? null : s.toLowerCase();
    }

    public static SearchCriteria empty() {
        return new SearchCriteria(null, null, null, null, null, null, null, null, null);
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
