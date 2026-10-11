package com.listings.search;

import java.util.List;

/** Thrown when search input is invalid. Carries every problem found, not just the first. */
public class InvalidSearchException extends RuntimeException {

    private final List<FieldError> errors;

    public InvalidSearchException(List<FieldError> errors) {
        super(buildMessage(errors));
        this.errors = List.copyOf(errors);
    }

    public InvalidSearchException(String field, String message) {
        this(List.of(new FieldError(field, message)));
    }

    public List<FieldError> getErrors() {
        return errors;
    }

    private static String buildMessage(List<FieldError> errors) {
        return errors.size() == 1
                ? errors.get(0).message()
                : errors.size() + " invalid search parameters";
    }
}
