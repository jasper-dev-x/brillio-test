package com.listings.search;

import static com.listings.search.TestListings.CLOCK;
import static com.listings.search.TestListings.TODAY;
import static com.listings.search.TestListings.listing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RelevanceScorerTest {

    private static final double EPS = 1e-9;
    private final RelevanceScorer scorer = new RelevanceScorer(CLOCK);

    // ---- budget component ----

    @Test
    void exactBudgetMatchGetsFullBudgetCredit() {
        assertEquals(1.0, scorer.budgetScore(500_000, 500_000), EPS);
    }

    @Test
    void underBudgetDecaysLinearlyToZeroAtFiftyPercentBelow() {
        assertEquals(0.8, scorer.budgetScore(450_000, 500_000), EPS);  // 10% under
        assertEquals(0.0, scorer.budgetScore(250_000, 500_000), EPS);  // 50% under
        assertEquals(0.0, scorer.budgetScore(100_000, 500_000), EPS);  // clamped, never negative
    }

    @Test
    void overBudgetIsPenalizedHarderThanUnderBudget() {
        assertEquals(0.6, scorer.budgetScore(550_000, 500_000), EPS);  // 10% over
        assertEquals(0.0, scorer.budgetScore(625_000, 500_000), EPS);  // 25% over
        assertTrue(scorer.budgetScore(550_000, 500_000) < scorer.budgetScore(450_000, 500_000));
    }

    // ---- recency component ----

    @Test
    void recencyHalvesEveryThirtyDays() {
        assertEquals(1.0, scorer.recencyScore(TODAY), EPS);
        assertEquals(0.5, scorer.recencyScore(TODAY.minusDays(30)), EPS);
        assertEquals(0.25, scorer.recencyScore(TODAY.minusDays(60)), EPS);
    }

    @Test
    void futureDatedListingIsTreatedAsListedToday() {
        assertEquals(1.0, scorer.recencyScore(TODAY.plusDays(10)), EPS);
    }

    @Test
    void missingListedDateGetsNoRecencyCredit() {
        assertEquals(0.0, scorer.recencyScore(null), EPS);
    }

    // ---- combined ----

    @Test
    void withoutTargetBudgetScoreIsRecencyOnly() {
        RelevanceScorer.Score s = scorer.score(listing("X", 500_000, 3, "Reston", TODAY.minusDays(30)), null);
        assertNull(s.budget());
        assertEquals(50.0, s.total(), EPS);
    }

    @Test
    void withTargetBudgetScoreBlendsBudgetAndRecency() {
        // budget 1.0 (exact), recency 0.5 (30 days) -> 0.7*1 + 0.3*0.5 = 0.85 -> 85.0
        RelevanceScorer.Score s = scorer.score(listing("X", 500_000, 3, "Reston", TODAY.minusDays(30)), 500_000L);
        assertEquals(1.0, s.budget(), EPS);
        assertEquals(0.5, s.recency(), EPS);
        assertEquals(85.0, s.total(), EPS);
    }

    @Test
    void scoreStaysWithinZeroToHundred() {
        RelevanceScorer.Score best = scorer.score(listing("X", 500_000, 3, "Reston", TODAY), 500_000L);
        RelevanceScorer.Score worst = scorer.score(listing("Y", 5_000_000, 3, "Reston", TODAY.minusYears(10)), 500_000L);
        assertEquals(100.0, best.total(), EPS);
        assertTrue(worst.total() >= 0.0 && worst.total() < 1.0);
    }
}
