package com.listings.web;

import java.util.List;

import com.listings.search.FieldError;

/** Uniform error body: { status, error, message, details: [{field, message}] } */
public record ApiError(int status, String error, String message, List<FieldError> details) {
}
