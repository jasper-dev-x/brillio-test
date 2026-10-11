package com.listings.search.dto;

/** One invalid request parameter, included in the {@code errors} property of a problem response. */
public record FieldErrorResponse(String field, String message) {
}
