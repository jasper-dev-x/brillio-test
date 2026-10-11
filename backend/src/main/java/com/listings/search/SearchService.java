package com.listings.search;

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

import com.listings.model.Listing;
import com.listings.repository.ListingRepository;

/**
 * Search pipeline: validate -> filter -> score -> sort -> paginate.
 * Scoring only happens after filtering, so we never score listings we'll throw away.
 */
@Service
public class SearchService {

    /**
     * Deterministic ordering. Highest score first; ties broken by newest listing,
     * then lowest price, then source/id so the same query always returns the same order
     * (important for stable pagination: no item can appear on two pages).
     */
    static final Comparator<Scored> RANKING = Comparator
            .comparingDouble((Scored s) -> s.score().total()).reversed()
            .thenComparing(s -> s.listing().listedDate(), Comparator.nullsLast(Comparator.<LocalDate>reverseOrder()))
            .thenComparingLong(s -> s.listing().price())
            .thenComparing(s -> s.listing().source())
            .thenComparing(s -> s.listing().id());

    private final ListingRepository repository;
    private final SearchValidator validator;
    private final RelevanceScorer scorer;
    private final Map<String, List<Listing>> duplicatesByKey;
    private final Set<String> knownCities;

    public SearchService(ListingRepository repository, SearchValidator validator,
                         RelevanceScorer scorer, DuplicateDetector duplicateDetector) {
        this.repository = repository;
        this.validator = validator;
        this.scorer = scorer;
        // Dataset is static, so duplicate groups are computed once rather than per request.
        this.duplicatesByKey = duplicateDetector.findDuplicates(repository.findAll());
        this.knownCities = repository.findAll().stream()
                .map(Listing::city)
                .filter(c -> c != null && !c.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
    }

    public SearchResult search(SearchCriteria criteria) {
        validator.validate(criteria);

        List<String> notices = new ArrayList<>();
        String city = criteria.normalizedCity();
        if (city != null && !knownCities.contains(city)) {
            // Valid request, just no data for that city: 200 + empty + an explicit explanation,
            // so the user can tell "typo / unknown city" apart from "filters too narrow".
            notices.add("No listings found in city '" + city + "'. Available cities: "
                    + String.join(", ", knownCities) + ".");
        }

        List<Scored> ranked = repository.findAll().stream()
                .filter(l -> matches(l, criteria))
                .map(l -> new Scored(l, scorer.score(l, criteria.targetBudget())))
                .sorted(RANKING)
                .toList();

        int page = criteria.pageOrDefault();
        int pageSize = criteria.pageSizeOrDefault();
        int totalItems = ranked.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);

        if (totalItems > 0 && page > totalPages) {
            throw new InvalidSearchException("page",
                    "page " + page + " is out of range; there " + (totalPages == 1 ? "is 1 page" : "are " + totalPages + " pages"));
        }

        int from = Math.min((page - 1) * pageSize, totalItems);
        int to = Math.min(from + pageSize, totalItems);
        List<SearchResult.Item> items = ranked.subList(from, to).stream().map(this::toItem).toList();

        return new SearchResult(items, page, pageSize, totalItems, totalPages, notices);
    }

    public List<String> knownCities() {
        return List.copyOf(knownCities);
    }

    boolean matches(Listing l, SearchCriteria c) {
        if (c.minPrice() != null && l.price() < c.minPrice()) return false;
        if (c.maxPrice() != null && l.price() > c.maxPrice()) return false;
        if (c.minBedrooms() != null && l.bedrooms() < c.minBedrooms()) return false;

        String city = c.normalizedCity();
        if (city != null && (l.city() == null || !l.city().trim().equalsIgnoreCase(city))) return false;

        String status = c.normalizedStatus();
        if (status != null && (l.status() == null || !l.status().equalsIgnoreCase(status))) return false;

        String keyword = c.normalizedKeyword();
        if (keyword != null) {
            String description = l.description() == null ? "" : l.description().toLowerCase(Locale.ROOT);
            // Every whitespace-separated term must appear (AND semantics), case-insensitive.
            for (String term : keyword.toLowerCase(Locale.ROOT).split("\\s+")) {
                if (!description.contains(term)) return false;
            }
        }
        return true;
    }

    private SearchResult.Item toItem(Scored s) {
        Listing l = s.listing();
        List<SearchResult.DuplicateRef> dups = duplicatesByKey.getOrDefault(l.key(), List.of()).stream()
                .map(d -> new SearchResult.DuplicateRef(d.id(), d.source(), d.address(), d.price()))
                .toList();
        return new SearchResult.Item(
                l.id(), l.source(), l.address(), l.city(), l.state(), l.zip(),
                l.price(), l.bedrooms(), l.bathrooms(), l.sqft(), l.latitude(), l.longitude(),
                l.listedDate(), l.status(), l.description(),
                s.score().total(),
                new SearchResult.ScoreBreakdown(s.score().budget(), s.score().recency()),
                dups);
    }

    record Scored(Listing listing, RelevanceScorer.Score score) {
    }
}
