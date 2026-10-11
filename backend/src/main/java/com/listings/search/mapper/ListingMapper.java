package com.listings.search.mapper;

import org.springframework.stereotype.Component;

import com.listings.search.config.SearchProperties;
import com.listings.search.dto.ListingResponse;
import com.listings.search.dto.ListingSearchRequest;
import com.listings.search.dto.SearchResponse;
import com.listings.search.model.Listing;
import com.listings.search.model.ListingStatus;
import com.listings.search.model.ScoredListing;
import com.listings.search.model.SearchCriteria;
import com.listings.search.model.SearchResult;

/** Translates between API DTOs and domain objects so neither leaks into the other layer. */
@Component
public class ListingMapper {

    private final SearchProperties searchProperties;

    public ListingMapper(SearchProperties searchProperties) {
        this.searchProperties = searchProperties;
    }

    /** Assumes the request already passed Bean Validation. Applies defaults and normalizes text. */
    public SearchCriteria toCriteria(ListingSearchRequest request) {
        return new SearchCriteria(
                request.getMinPrice(),
                request.getMaxPrice(),
                request.getMinBedrooms(),
                SearchCriteria.blankToNull(request.getCity()),
                SearchCriteria.blankToNull(request.getKeyword()),
                request.getTargetBudget(),
                ListingStatus.parse(SearchCriteria.blankToNull(request.getStatus())).orElse(null),
                request.getPage() == null ? 1 : request.getPage(),
                request.getPageSize() == null ? searchProperties.defaultPageSize() : request.getPageSize());
    }

    public SearchResponse toResponse(SearchResult result) {
        return new SearchResponse(
                result.items().stream().map(this::toResponse).toList(),
                result.page(),
                result.pageSize(),
                result.totalItems(),
                result.totalPages(),
                result.notices());
    }

    ListingResponse toResponse(ScoredListing scored) {
        Listing l = scored.listing();
        return new ListingResponse(
                l.id(), l.source(), l.address(), l.city(), l.state(), l.zip(),
                l.price(), l.bedrooms(), l.bathrooms(), l.sqft(), l.latitude(), l.longitude(),
                l.listedDate(),
                l.status() == null ? null : l.status().value(),
                l.description(),
                scored.score().total(),
                new ListingResponse.ScoreBreakdown(scored.score().budget(), scored.score().recency()),
                scored.possibleDuplicates().stream()
                        .map(d -> new ListingResponse.DuplicateRef(d.id(), d.source(), d.address(), d.price()))
                        .toList());
    }
}
