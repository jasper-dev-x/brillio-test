package com.listings.search.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.listings.search.model.Listing;

/**
 * Loads listings once from a JSON resource at startup and serves them from memory.
 * The dataset is small and read-only, so a database would add operational cost with no benefit.
 */
@Repository
public class JsonFileListingRepository implements ListingRepository {

    private static final Logger log = LoggerFactory.getLogger(JsonFileListingRepository.class);

    private final List<Listing> listings;

    public JsonFileListingRepository(@Value("${listings.data-location}") Resource dataLocation,
                                     ObjectMapper objectMapper) {
        try (InputStream in = dataLocation.getInputStream()) {
            this.listings = List.copyOf(objectMapper.readValue(in, new TypeReference<List<Listing>>() { }));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load listings from " + dataLocation, e);
        }
        log.info("Loaded {} listings from {}", listings.size(), dataLocation);
    }

    @Override
    public List<Listing> findAll() {
        return listings;
    }
}
