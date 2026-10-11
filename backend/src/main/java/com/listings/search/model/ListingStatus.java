package com.listings.search.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Listing lifecycle status. Serialized in lower case to match the feed format ("active"). */
public enum ListingStatus {
    ACTIVE,
    PENDING,
    SOLD;

    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Case-insensitive lookup; empty for unknown values. */
    public static Optional<ListingStatus> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).filter(s -> s.name().equals(normalized)).findFirst();
    }

    @JsonCreator
    public static ListingStatus fromJson(String raw) {
        return parse(raw).orElseThrow(() -> new IllegalArgumentException("Unknown listing status: " + raw));
    }
}
