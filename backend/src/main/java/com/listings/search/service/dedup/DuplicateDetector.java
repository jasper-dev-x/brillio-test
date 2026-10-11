package com.listings.search.service.dedup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.listings.search.model.Listing;

/**
 * Flags listings that are probably the same property reported by different feeds,
 * e.g. "123 Main St, Apt 4B" (MLS_A) vs "123 Main Street, Unit 4B" (MLS_B).
 *
 * <p>Deliberately <b>flags</b> rather than merges: picking a winning record means deciding
 * whose price/date/description is authoritative, which is a business rule. Flagging keeps
 * every source visible and leaves that decision open.
 *
 * <p>Match rule: normalized street address + normalized city. Zip is ignored on purpose —
 * the sample data lists the same house under 22150 and 22151.
 */
@Component
public class DuplicateDetector {

    private static final Map<String, String> TOKEN_ALIASES = new HashMap<>();

    static {
        // Street suffixes -> USPS abbreviations
        alias("st", "street", "str");
        alias("ave", "avenue", "av");
        alias("rd", "road");
        alias("ln", "lane");
        alias("ct", "court");
        alias("dr", "drive");
        alias("blvd", "boulevard");
        alias("way", "wy");
        alias("pl", "place");
        alias("cir", "circle");
        alias("ter", "terrace");
        alias("hwy", "highway");
        alias("pkwy", "parkway");
        // Directionals
        alias("n", "north");
        alias("s", "south");
        alias("e", "east");
        alias("w", "west");
        // Unit designators collapse to one token: "Apt 4B" == "Unit 4B" == "#4B"
        alias("unit", "apt", "apartment", "suite", "ste", "#");
    }

    private static void alias(String canonical, String... variants) {
        TOKEN_ALIASES.put(canonical, canonical);
        for (String variant : variants) {
            TOKEN_ALIASES.put(variant, canonical);
        }
    }

    /**
     * @return listing key ({@link Listing#key()}) to the <em>other</em> listings that look like the
     *         same property. Every input listing has an entry; non-duplicates map to an empty list.
     */
    public Map<String, List<Listing>> findDuplicates(List<Listing> listings) {
        Map<String, List<Listing>> groups = new LinkedHashMap<>();
        for (Listing listing : listings) {
            groups.computeIfAbsent(propertyKey(listing), k -> new ArrayList<>()).add(listing);
        }
        Map<String, List<Listing>> result = new HashMap<>();
        for (List<Listing> group : groups.values()) {
            for (Listing listing : group) {
                result.put(listing.key(), group.stream().filter(other -> other != listing).toList());
            }
        }
        return result;
    }

    static String propertyKey(Listing listing) {
        return normalizeAddress(listing.address()) + "|" + normalizeText(listing.city());
    }

    static String normalizeAddress(String address) {
        if (address == null) {
            return "";
        }
        String cleaned = address.toLowerCase(Locale.ROOT)
                .replace("#", " # ")
                .replaceAll("[.,]", " ");
        StringBuilder normalized = new StringBuilder();
        for (String token : cleaned.trim().split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            if (!normalized.isEmpty()) {
                normalized.append(' ');
            }
            normalized.append(TOKEN_ALIASES.getOrDefault(token, token));
        }
        return normalized.toString();
    }

    static String normalizeText(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
