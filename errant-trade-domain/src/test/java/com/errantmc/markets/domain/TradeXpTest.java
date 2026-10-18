package com.errantmc.markets.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TradeXpTest {

    @Test
    void experienceScalesWithTheTradesWorth() {
        assertEquals(2, TradeXp.award(100, 0, 50).xp());
        assertEquals(20, TradeXp.award(1000, 0, 50).xp());
    }

    /** Ten trades of 10 bits are worth the same as one of 100: nothing is lost to rounding. */
    @Test
    void smallTradesAddUpThroughTheRemainder() {
        long carried = 0;
        int total = 0;
        for (int i = 0; i < 10; i++) {
            TradeXp award = TradeXp.award(10, carried, 50);
            total += award.xp();
            carried = award.remainder();
        }
        assertEquals(TradeXp.award(100, 0, 50).xp(), total);
        assertEquals(0, carried);
    }

    @Test
    void aTradeTooSmallForAPointCarriesItAll() {
        TradeXp award = TradeXp.award(30, 0, 50);

        assertEquals(0, award.xp());
        assertEquals(30, award.remainder());
    }

    @Test
    void theRateMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> TradeXp.award(100, 0, 0));
    }
}
