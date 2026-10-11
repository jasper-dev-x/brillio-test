package com.listings.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.listings.search.SearchCriteria;
import com.listings.search.SearchResult;
import com.listings.search.SearchService;

@RestController
@RequestMapping("/api")
public class ListingController {

    private final SearchService searchService;

    public ListingController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * GET /api/listings?minPrice=&maxPrice=&minBedrooms=&city=&keyword=&targetBudget=&status=&page=&pageSize=
     * All parameters optional. Returns 400 with field-level details for invalid input.
     */
    @GetMapping("/listings")
    public SearchResult search(
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) Integer minBedrooms,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long targetBudget,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return searchService.search(new SearchCriteria(
                minPrice, maxPrice, minBedrooms, city, keyword, targetBudget, status, page, pageSize));
    }

    /** Distinct cities in the dataset; the UI uses it for autocomplete suggestions. */
    @GetMapping("/cities")
    public List<String> cities() {
        return searchService.knownCities();
    }
}
