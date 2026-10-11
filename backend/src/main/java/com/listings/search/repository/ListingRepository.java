package com.listings.search.repository;

import java.util.List;

import com.listings.search.model.Listing;

/**
 * Read access to listings. The search layer depends only on this interface,
 * so the JSON-file implementation can be swapped for JPA/JDBC or a search index
 * without touching business logic.
 */
public interface ListingRepository {

    List<Listing> findAll();
}
