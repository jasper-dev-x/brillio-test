package com.listings.search.service.scoring;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

import com.listings.search.config.ScoringProperties;
import com.listings.search.model.Listing;
import com.listings.search.model.RelevanceScore;

/**
 * Relevance = weighted blend of two components, each normalized to 0..1.
 *
 * <ul>
 *   <li><b>Budget fit</b> — linear falloff by relative distance from {@code targetBudget}, asymmetric:
 *       full credit at the target, zero at {@code underBudgetTolerance} below (default 50%) or
 *       {@code overBudgetTolerance} above (default 25%). Over budget hurts more because the buyer
 *       may not be able to afford it; under budget is still a viable home.</li>
 *   <li><b>Recency</b> — exponential decay with a half-life (default 30 days): today = 1.0,
 *       30 days = 0.5, 60 days = 0.25. Never reaches 0, so it still orders older listings.</li>
 * </ul>
 *
 * <pre>
 *   score = 100 × (budgetWeight × budgetFit + recencyWeight × recency)   with targetBudget
 *   score = 100 × recency                                              without targetBudget
 * </pre>
 *
 * Scores are rounded to 2 decimals so visually equal scores compare equal and the
 * deterministic tie-break order (see service) decides, not floating-point noise.
 */
@Component
public class RelevanceScorer {

    private final Clock clock;
    private final ScoringProperties properties;

    public RelevanceScorer(Clock clock, ScoringProperties properties) {
        this.clock = clock;
        this.properties = properties;
    }

    public RelevanceScore score(Listing listing, BigDecimal targetBudget) {
        double recency = recencyScore(listing.listedDate());
        if (targetBudget == null) {
            return new RelevanceScore(round(recency * 100), null, round(recency));
        }
        double budget = budgetScore(listing.price(), targetBudget);
        double totalWeight = properties.budgetWeight() + properties.recencyWeight();
        if (totalWeight <= 0) {
            return new RelevanceScore(round(recency * 100), round(budget), round(recency));
        }
        // Normalizing by the weight sum keeps the result on 0..100 even if weights don't add to 1.
        double blended = (properties.budgetWeight() * budget + properties.recencyWeight() * recency) / totalWeight;
        return new RelevanceScore(round(blended * 100), round(budget), round(recency));
    }

    double budgetScore(BigDecimal price, BigDecimal targetBudget) {
        if (price == null) {
            return 0.0;
        }
        double target = targetBudget.doubleValue();
        double deviation = (price.doubleValue() - target) / target; // negative = under budget
        double tolerance = deviation <= 0 ? properties.underBudgetTolerance() : properties.overBudgetTolerance();
        return Math.max(0.0, 1.0 - Math.abs(deviation) / tolerance);
    }

    double recencyScore(LocalDate listedDate) {
        if (listedDate == null) {
            return 0.0; // unknown date earns no recency credit rather than a guessed one
        }
        long ageDays = Math.max(0, ChronoUnit.DAYS.between(listedDate, LocalDate.now(clock)));
        return Math.pow(0.5, ageDays / properties.recencyHalfLifeDays());
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
