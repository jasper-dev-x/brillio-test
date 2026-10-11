package com.listings.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.listings.model.Listing;

/**
 * Flags listings that are probably the same property reported by different feeds
 * (e.g. "123 Main St, Apt 4B" from MLS_A vs "123 Main Street, Unit 4B" from MLS_B).
 *
 * Deliberately FLAGS rather than merges: picking a "winning" record means choosing
 * whose price/date/description is correct, which is a business decision. Flagging
 * keeps every source's data visible and leaves that decision to the user.
 *
 * Two listings are considered duplicates when their normalized address + city match.
 * Zip is ignored on purpose: the sample data has the same house under 22150 and 22151.
 */
@Component
public class DuplicateDetector {

    private static final Map<String, String> TOKEN_ALIASES = new HashMap<>();

    static {
        // Street suffixes -> USPS abbreviation
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
        // Unit designators all collapse to one token so "Apt 4B" == "Unit 4B" == "#4B"
        alias("unit", "apt", "apartment", "suite", "ste", "#");
    }

    private static void alias(String canonical, String... variants) {
        TOKEN_ALIASES.put(canonical, canonical);
        for (String v : variants) {
            TOKEN_ALIASES.put(v, canonical);
        }
    }

    /**
     * @return map of listing key -> the OTHER listings that look like the same property.
     *         Listings with no duplicates map to an empty list.
     */
    public Map<String, List<Listing>> findDuplicates(List<Listing> listings) {
        Map<String, List<Listing>> groups = new LinkedHashMap<>();
        for (Listing l : listings) {
            groups.computeIfAbsent(propertyKey(l), k -> new ArrayList<>()).add(l);
        }
        Map<String, List<Listing>> result = new HashMap<>();
        for (List<Listing> group : groups.values()) {
            for (Listing l : group) {
                List<Listing> others = group.stream().filter(o -> o != l).toList();
                result.put(l.key(), others);
            }
        }
        return result;
    }

    String propertyKey(Listing l) {
        return normalizeAddress(l.address()) + "|" + normalizeText(l.city());
    }

    static String normalizeAddress(String address) {
        if (address == null) return "";
        String cleaned = address.toLowerCase(Locale.ROOT)
                .replace("#", " # ")
                .replaceAll("[.,]", " ");
        StringBuilder sb = new StringBuilder();
        for (String token : cleaned.trim().split("\\s+")) {
            if (token.isEmpty()) continue;
            String canonical = TOKEN_ALIASES.getOrDefault(token, token);
            if (sb.length() > 0) sb.append(' ');
            sb.append(canonical);
        }
        return sb.toString();
    }

    static String normalizeText(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
