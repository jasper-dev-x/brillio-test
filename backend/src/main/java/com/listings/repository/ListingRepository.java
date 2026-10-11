package com.listings.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.listings.model.Listing;

/**
 * In-memory listing store, loaded once from JSON at startup.
 * The dataset is small and read-only, so a database would add cost without benefit;
 * swapping this for a JPA/JDBC repository would not change the search layer.
 */
@Repository
public class ListingRepository {

    private final List<Listing> listings;

    @Autowired
    public ListingRepository(@Value("${listings.data-location}") Resource dataLocation, ObjectMapper objectMapper) {
        try (InputStream in = dataLocation.getInputStream()) {
            this.listings = List.copyOf(objectMapper.readValue(in, new TypeReference<List<Listing>>() {}));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load listings from " + dataLocation, e);
        }
    }

    /** Test-friendly constructor. */
    public ListingRepository(List<Listing> listings) {
        this.listings = List.copyOf(listings);
    }

    public List<Listing> findAll() {
        return listings;
    }
}
