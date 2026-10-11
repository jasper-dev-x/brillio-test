package com.listings.search.service;

import java.util.List;

import com.listings.search.model.SearchCriteria;
import com.listings.search.model.SearchResult;

/** Searches, ranks and paginates listings. */
public interface ListingSearchService {

    /**
     * @throws com.listings.search.exception.PageOutOfRangeException if the page is past the last page
     */
    SearchResult search(SearchCriteria criteria);

    /** Distinct city names present in the data, sorted case-insensitively. */
    List<String> findCities();
}
