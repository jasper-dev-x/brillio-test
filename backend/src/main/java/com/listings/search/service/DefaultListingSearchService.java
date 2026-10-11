package com.listings.search.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.listings.search.exception.PageOutOfRangeException;
import com.listings.search.model.Listing;
import com.listings.search.model.RelevanceScore;
import com.listings.search.model.ScoredListing;
import com.listings.search.model.SearchCriteria;
import com.listings.search.model.SearchResult;
import com.listings.search.repository.ListingRepository;
import com.listings.search.service.dedup.DuplicateDetector;
import com.listings.search.service.scoring.RelevanceScorer;

/**
 * Pipeline: filter → score → sort → paginate. Input has already passed request validation.
 * Scoring runs only on listings that survive filtering.
 */
@Service
public class DefaultListingSearchService implements ListingSearchService {

    /**
     * Deterministic total order: highest score first, then newest, then cheapest, then source/id.
     * A stable order means the same query always yields the same pages (no item on two pages).
     */
    static final Comparator<Scored> RANKING = Comparator
            .comparingDouble((Scored s) -> s.score().total()).reversed()
            .thenComparing(s -> s.listing().listedDate(), Comparator.nullsLast(Comparator.<LocalDate>reverseOrder()))
            .thenComparing(s -> s.listing().price(), Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(s -> s.listing().source(), Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(s -> s.listing().id(), Comparator.nullsLast(Comparator.naturalOrder()));

    private final ListingRepository repository;
    private final RelevanceScorer scorer;
    private final Map<String, List<Listing>> duplicatesByKey;
    private final Set<String> knownCities;

    public DefaultListingSearchService(ListingRepository repository, RelevanceScorer scorer,
                                       DuplicateDetector duplicateDetector) {
        this.repository = repository;
        this.scorer = scorer;
        // The dataset is static, so duplicate groups are computed once rather than per request.
        this.duplicatesByKey = duplicateDetector.findDuplicates(repository.findAll());
        this.knownCities = repository.findAll().stream()
                .map(Listing::city)
                .map(SearchCriteria::blankToNull)
                .filter(c -> c != null)
                .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
    }

    @Override
    public SearchResult search(SearchCriteria criteria) {
        List<String> notices = new ArrayList<>();
        if (criteria.city() != null && !knownCities.contains(criteria.city())) {
            // A well-formed request with no data for that city: 200 + empty + explanation, so the
            // user can tell "unknown city / typo" apart from "filters too narrow".
            notices.add("No listings found in city '" + criteria.city() + "'. Available cities: "
                    + String.join(", ", knownCities) + ".");
        }

        List<Scored> ranked = repository.findAll().stream()
                .filter(listing -> matches(listing, criteria))
                .map(listing -> new Scored(listing, scorer.score(listing, criteria.targetBudget())))
                .sorted(RANKING)
                .toList();

        int totalItems = ranked.size();
        int totalPages = (int) Math.ceil((double) totalItems / criteria.pageSize());
        if (totalItems > 0 && criteria.page() > totalPages) {
            throw new PageOutOfRangeException(criteria.page(), totalPages);
        }

        int from = Math.min((criteria.page() - 1) * criteria.pageSize(), totalItems);
        int to = Math.min(from + criteria.pageSize(), totalItems);
        List<ScoredListing> page = ranked.subList(from, to).stream()
                .map(s -> new ScoredListing(s.listing(), s.score(),
                        duplicatesByKey.getOrDefault(s.listing().key(), List.of())))
                .toList();

        return new SearchResult(page, criteria.page(), criteria.pageSize(), totalItems, totalPages, notices);
    }

    @Override
    public List<String> findCities() {
        return List.copyOf(knownCities);
    }

    static boolean matches(Listing listing, SearchCriteria c) {
        if (c.minPrice() != null && (listing.price() == null || listing.price().compareTo(c.minPrice()) < 0)) {
            return false;
        }
        if (c.maxPrice() != null && (listing.price() == null || listing.price().compareTo(c.maxPrice()) > 0)) {
            return false;
        }
        if (c.minBedrooms() != null && listing.bedrooms() < c.minBedrooms()) {
            return false;
        }
        if (c.city() != null && (listing.city() == null || !listing.city().trim().equalsIgnoreCase(c.city()))) {
            return false;
        }
        if (c.status() != null && listing.status() != c.status()) {
            return false;
        }
        if (c.keyword() != null) {
            String description = listing.description() == null ? "" : listing.description().toLowerCase(Locale.ROOT);
            // Every whitespace-separated term must appear (AND semantics), case-insensitive.
            for (String term : c.keyword().toLowerCase(Locale.ROOT).split("\\s+")) {
                if (!description.contains(term)) {
                    return false;
                }
            }
        }
        return true;
    }

    record Scored(Listing listing, RelevanceScore score) {
    }
}
