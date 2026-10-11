package com.listings.search.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Query parameters for {@code GET /api/v1/listings}, bound via {@code @ModelAttribute}.
 * All fields are optional. Constraints are enforced with Jakarta Bean Validation; violations
 * become an RFC 9457 problem response listing every invalid field.
 */
@ValidPriceRange
public class ListingSearchRequest {

    public static final int MAX_PAGE_SIZE = 50;
    public static final int MAX_TEXT_LENGTH = 100;

    @Schema(description = "Minimum list price (USD, inclusive)", example = "400000")
    @PositiveOrZero(message = "minPrice must be 0 or greater")
    private BigDecimal minPrice;

    @Schema(description = "Maximum list price (USD, inclusive)", example = "600000")
    @PositiveOrZero(message = "maxPrice must be 0 or greater")
    private BigDecimal maxPrice;

    @Schema(description = "Minimum bedrooms (inclusive)", example = "2")
    @PositiveOrZero(message = "minBedrooms must be 0 or greater")
    private Integer minBedrooms;

    @Schema(description = "City, case-insensitive exact match", example = "Springfield")
    @Size(max = MAX_TEXT_LENGTH, message = "city cannot be longer than " + MAX_TEXT_LENGTH + " characters")
    private String city;

    @Schema(description = "Words that must all appear in the description (case-insensitive)", example = "fenced yard")
    @Size(max = MAX_TEXT_LENGTH, message = "keyword cannot be longer than " + MAX_TEXT_LENGTH + " characters")
    private String keyword;

    @Schema(description = "Target price used for relevance ranking", example = "500000")
    @Positive(message = "targetBudget must be greater than 0")
    private BigDecimal targetBudget;

    @Schema(description = "Listing status", allowableValues = {"active", "pending", "sold"})
    @Pattern(regexp = "(?i)\\s*(active|pending|sold)\\s*", message = "status must be one of active, pending, sold")
    private String status;

    @Schema(description = "1-based page number", defaultValue = "1")
    @Min(value = 1, message = "page must be 1 or greater")
    private Integer page;

    @Schema(description = "Results per page (1-" + MAX_PAGE_SIZE + ")", defaultValue = "5")
    @Min(value = 1, message = "pageSize must be 1 or greater")
    @Max(value = MAX_PAGE_SIZE, message = "pageSize cannot exceed " + MAX_PAGE_SIZE)
    private Integer pageSize;

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

    public Integer getMinBedrooms() { return minBedrooms; }
    public void setMinBedrooms(Integer minBedrooms) { this.minBedrooms = minBedrooms; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public BigDecimal getTargetBudget() { return targetBudget; }
    public void setTargetBudget(BigDecimal targetBudget) { this.targetBudget = targetBudget; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}
