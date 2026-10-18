package com.errantmc.markets.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingTest {

    @Test
    void fullStockSellsAtTheBasePrice() {
        assertEquals(100, Pricing.scarce(100, 6, 6, 5, 50));
    }

    @Test
    void eachSaleRaisesThePrice() {
        assertEquals(105, Pricing.scarce(100, 6, 5, 5, 50));
        assertEquals(120, Pricing.scarce(100, 6, 2, 5, 50));
    }

    @Test
    void theRiseIsCapped() {
        assertEquals(150, Pricing.scarce(100, 64, 0, 5, 50));
    }

    @Test
    void unlimitedStockNeverRises() {
        assertEquals(100, Pricing.scarce(100, -1, -1, 5, 50));
    }

    @Test
    void moreStockThanRolledIsNotACheaperPrice() {
        assertEquals(100, Pricing.scarce(100, 4, 9, 5, 50)); // bought back from the player
    }

    @Test
    void aComfortableTillPaysTheFullPrice() {
        assertEquals(100, Pricing.shortOfMoney(100, 1000, 1000, 50));
        assertEquals(100, Pricing.shortOfMoney(100, 5000, 1000, 50));
    }

    @Test
    void aShortTillPaysLessInAStraightLine() {
        assertEquals(75, Pricing.shortOfMoney(100, 500, 1000, 50));
        assertEquals(50, Pricing.shortOfMoney(100, 0, 1000, 50));
    }

    @Test
    void aPriceNeverFallsToNothing() {
        assertEquals(1, Pricing.shortOfMoney(1, 0, 1000, 0));
        assertEquals(0, Pricing.shortOfMoney(0, 0, 1000, 50));
    }
}
