package com.listings.search;

/** A single validation problem tied to the request parameter that caused it. */
public record FieldError(String field, String message) {
}
