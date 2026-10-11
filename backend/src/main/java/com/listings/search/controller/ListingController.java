package com.listings.search.controller;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.listings.search.dto.ListingSearchRequest;
import com.listings.search.dto.SearchResponse;
import com.listings.search.mapper.ListingMapper;
import com.listings.search.service.ListingSearchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Listings", description = "Search and rank property listings")
public class ListingController {

    private final ListingSearchService searchService;
    private final ListingMapper mapper;

    public ListingController(ListingSearchService searchService, ListingMapper mapper) {
        this.searchService = searchService;
        this.mapper = mapper;
    }

    @Operation(summary = "Search listings",
            description = "Filters, ranks by relevance (budget fit + recency) and paginates listings.")
    @ApiResponse(responseCode = "200", description = "A page of ranked results")
    @ApiResponse(responseCode = "400", description = "Invalid parameters",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/listings")
    public SearchResponse search(@ParameterObject @Valid @ModelAttribute ListingSearchRequest request) {
        return mapper.toResponse(searchService.search(mapper.toCriteria(request)));
    }

    @Operation(summary = "List cities", description = "Distinct cities present in the data, for autocomplete.")
    @GetMapping("/cities")
    public List<String> cities() {
        return searchService.findCities();
    }
}
