package com.listings.search.service.scoring;

import static com.listings.search.support.TestListings.FIXED_CLOCK;
import static com.listings.search.support.TestListings.TODAY;
import static com.listings.search.support.TestListings.listing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.listings.search.config.ScoringProperties;
import com.listings.search.model.RelevanceScore;

class RelevanceScorerTest {

    private static final double EPS = 1e-9;
    private static final BigDecimal TARGET = BigDecimal.valueOf(500_000);

    private final RelevanceScorer scorer = new RelevanceScorer(FIXED_CLOCK, ScoringProperties.defaults());

    private double budget(long price) {
        return scorer.budgetScore(BigDecimal.valueOf(price), TARGET);
    }

    @Nested
    @DisplayName("budget fit")
    class BudgetFit {

        @Test
        void exactMatchGetsFullCredit() {
            assertThat(budget(500_000)).isCloseTo(1.0, within(EPS));
        }

        @Test
        void underBudgetFallsOffLinearlyToZeroAtFiftyPercentBelow() {
            assertThat(budget(450_000)).isCloseTo(0.8, within(EPS));
            assertThat(budget(250_000)).isCloseTo(0.0, within(EPS));
        }

        @Test
        void overBudgetFallsOffToZeroAtTwentyFivePercentAbove() {
            assertThat(budget(550_000)).isCloseTo(0.6, within(EPS));
            assertThat(budget(625_000)).isCloseTo(0.0, within(EPS));
        }

        @Test
        void overBudgetIsPenalizedMoreThanEquallyFarUnderBudget() {
            assertThat(budget(550_000)).isLessThan(budget(450_000));
        }

        @Test
        void neverGoesNegative() {
            assertThat(budget(5_000_000)).isCloseTo(0.0, within(EPS));
            assertThat(budget(1)).isCloseTo(0.0, within(EPS));
        }
    }

    @Nested
    @DisplayName("recency")
    class Recency {

        @Test
        void halvesEveryThirtyDays() {
            assertThat(scorer.recencyScore(TODAY)).isCloseTo(1.0, within(EPS));
            assertThat(scorer.recencyScore(TODAY.minusDays(30))).isCloseTo(0.5, within(EPS));
            assertThat(scorer.recencyScore(TODAY.minusDays(60))).isCloseTo(0.25, within(EPS));
        }

        @Test
        void futureDatedListingCountsAsListedToday() {
            assertThat(scorer.recencyScore(TODAY.plusDays(10))).isCloseTo(1.0, within(EPS));
        }

        @Test
        void missingDateEarnsNoCredit() {
            assertThat(scorer.recencyScore(null)).isCloseTo(0.0, within(EPS));
        }
    }

    @Nested
    @DisplayName("blended score")
    class Blended {

        @Test
        void withoutTargetBudgetScoreIsRecencyOnly() {
            RelevanceScore score = scorer.score(listing("X").daysAgo(30).build(), null);

            assertThat(score.budget()).isNull();
            assertThat(score.total()).isCloseTo(50.0, within(EPS));
        }

        @Test
        void withTargetBudgetBlendsSeventyThirty() {
            // budget 1.0, recency 0.5 -> 0.7 * 1.0 + 0.3 * 0.5 = 0.85
            RelevanceScore score = scorer.score(listing("X").price(500_000).daysAgo(30).build(), TARGET);

            assertThat(score.budget()).isCloseTo(1.0, within(EPS));
            assertThat(score.recency()).isCloseTo(0.5, within(EPS));
            assertThat(score.total()).isCloseTo(85.0, within(EPS));
        }

        @Test
        void staysWithinZeroToOneHundred() {
            RelevanceScore best = scorer.score(listing("A").price(500_000).build(), TARGET);
            RelevanceScore worst = scorer.score(listing("B").price(9_000_000).listedDate(TODAY.minusYears(10)).build(), TARGET);

            assertThat(best.total()).isCloseTo(100.0, within(EPS));
            assertThat(worst.total()).isBetween(0.0, 1.0);
        }

        @Test
        void weightsAreConfigurable() {
            RelevanceScorer budgetOnly = new RelevanceScorer(FIXED_CLOCK, new ScoringProperties(1.0, 0.0, 0.5, 0.25, 30));
            RelevanceScore score = budgetOnly.score(listing("X").price(450_000).daysAgo(90).build(), TARGET);

            assertThat(score.total()).isCloseTo(80.0, within(EPS));
        }
    }
}
