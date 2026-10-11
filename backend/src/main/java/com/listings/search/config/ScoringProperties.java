package com.listings.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;

/**
 * Tunable relevance-scoring parameters ({@code listings.scoring.*}).
 * Externalized so weights can be tuned per environment without a code change;
 * validated at startup so a bad value fails fast instead of producing silent garbage rankings.
 *
 * @param budgetWeight          weight of budget fit in the blended score (0..1)
 * @param recencyWeight         weight of recency in the blended score (0..1)
 * @param underBudgetTolerance  fraction below target at which budget fit reaches 0
 * @param overBudgetTolerance   fraction above target at which budget fit reaches 0
 * @param recencyHalfLifeDays   days for the recency score to halve
 */
@Validated
@ConfigurationProperties(prefix = "listings.scoring")
public record ScoringProperties(
        @DefaultValue("0.7") @DecimalMin("0.0") @DecimalMax("1.0") double budgetWeight,
        @DefaultValue("0.3") @DecimalMin("0.0") @DecimalMax("1.0") double recencyWeight,
        @DefaultValue("0.5") @Positive double underBudgetTolerance,
        @DefaultValue("0.25") @Positive double overBudgetTolerance,
        @DefaultValue("30") @Positive double recencyHalfLifeDays) {

    /** The documented defaults; used by unit tests that don't start Spring. */
    public static ScoringProperties defaults() {
        return new ScoringProperties(0.7, 0.3, 0.5, 0.25, 30);
    }
}
