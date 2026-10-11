package com.listings.search;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.listings.model.Listing;

/**
 * Relevance = weighted blend of two components, each normalized to 0..1:
 *
 *  budgetFit — how close the price is to targetBudget, asymmetric:
 *      under budget: loses full credit linearly at 50% below target
 *      over budget:  loses full credit linearly at 25% above target
 *      (being over budget hurts a buyer more than being under it)
 *
 *  recency — exponential decay by listing age, half-life 30 days:
 *      listed today = 1.0, 30 days ago = 0.5, 60 days ago = 0.25 ...
 *      Never hits 0, so recency still orders old listings among themselves.
 *
 *  score = 0.7 * budgetFit + 0.3 * recency       (targetBudget given)
 *  score = recency                               (no targetBudget)
 *
 * Final score is reported on a 0..100 scale. Budget is weighted higher because
 * it reflects an explicit user preference; recency is an implicit freshness signal.
 */
@Component
public class RelevanceScorer {

    public static final double BUDGET_WEIGHT = 0.7;
    public static final double RECENCY_WEIGHT = 0.3;
    public static final double UNDER_BUDGET_TOLERANCE = 0.50;
    public static final double OVER_BUDGET_TOLERANCE = 0.25;
    public static final double RECENCY_HALF_LIFE_DAYS = 30.0;

    private final Clock clock;

    @Autowired
    public RelevanceScorer(Clock clock) {
        this.clock = clock;
    }

    public Score score(Listing listing, Long targetBudget) {
        double recency = recencyScore(listing.listedDate());
        if (targetBudget == null) {
            return new Score(round(recency * 100), null, round(recency));
        }
        double budget = budgetScore(listing.price(), targetBudget);
        double combined = BUDGET_WEIGHT * budget + RECENCY_WEIGHT * recency;
        return new Score(round(combined * 100), round(budget), round(recency));
    }

    double budgetScore(long price, long targetBudget) {
        double deviation = (double) (price - targetBudget) / targetBudget; // negative = under budget
        double tolerance = deviation <= 0 ? UNDER_BUDGET_TOLERANCE : OVER_BUDGET_TOLERANCE;
        return Math.max(0.0, 1.0 - Math.abs(deviation) / tolerance);
    }

    double recencyScore(LocalDate listedDate) {
        if (listedDate == null) {
            return 0.0; // unknown date: no recency credit rather than a guess
        }
        long ageDays = ChronoUnit.DAYS.between(listedDate, LocalDate.now(clock));
        ageDays = Math.max(0, ageDays); // future-dated listings are treated as listed today
        return Math.pow(0.5, ageDays / RECENCY_HALF_LIFE_DAYS);
    }

    /**
     * Rounded so equal-looking scores actually compare equal and tie-breaking is
     * visible and predictable (floating-point noise would otherwise decide order).
     */
    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /** @param budget null when no targetBudget was supplied */
    public record Score(double total, Double budget, double recency) {
    }
}
