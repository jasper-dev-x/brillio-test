package com.listings.search.model;

/**
 * Relevance score on a 0..100 scale with its components (each 0..1).
 *
 * @param total   blended score, 0..100, rounded to 2 decimals
 * @param budget  budget-fit component; {@code null} when no target budget was supplied
 * @param recency recency component
 */
public record RelevanceScore(double total, Double budget, double recency) {
}
