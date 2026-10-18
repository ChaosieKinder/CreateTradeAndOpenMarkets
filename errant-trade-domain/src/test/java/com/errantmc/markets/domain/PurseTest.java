package com.errantmc.markets.domain;

import org.junit.jupiter.api.Test;

import static com.errantmc.markets.domain.CoinDenomination.*;
import static org.junit.jupiter.api.Assertions.*;

class PurseTest {

    @Test
    void valueIsCountedInCopperBits() {
        Purse p = Purse.of(SILVER_CROWN, 3).add(COPPER_BIT, 2).updated();

        assertEquals(302, p.totalValue());
    }

    @Test
    void theLargestPossiblePurseDoesNotOverflowItsValue() {
        Purse p = Purse.EMPTY;
        for (CoinDenomination d : CoinDenomination.coins()) p = p.with(d, Integer.MAX_VALUE);

        long expected = 0;
        for (CoinDenomination d : CoinDenomination.coins()) expected += (long) Integer.MAX_VALUE * d.value;
        assertEquals(expected, p.totalValue());
        assertEquals(8L * Integer.MAX_VALUE, p.totalCount());
    }

    @Test
    void removeTakesOnlyWhatIsThereAndReportsTheRest() {
        var change = Purse.of(GOLD_MARK, 2).remove(GOLD_MARK, 5);

        assertEquals(2, change.moved());
        assertEquals(3, change.remaining());
        assertEquals(0, change.updated().count(GOLD_MARK));
    }

    @Test
    void addTakesWhatFitsUnderIntMaxAndReportsTheRest() {
        var change = Purse.of(COPPER_BIT, Integer.MAX_VALUE - 1).add(COPPER_BIT, 5);

        assertEquals(1, change.moved());
        assertEquals(4, change.remaining());
        assertEquals(Integer.MAX_VALUE, change.updated().count(COPPER_BIT));
    }

    @Test
    void mergingPursesThatWouldOverflowIsRefusedNotWrapped() {
        Purse nearlyFull = Purse.of(EMERALD_SUN, Integer.MAX_VALUE - 1);

        assertTrue(nearlyFull.canAdd(Purse.of(EMERALD_SUN, 1)));
        assertEquals(Integer.MAX_VALUE, nearlyFull.add(Purse.of(EMERALD_SUN, 1)).count(EMERALD_SUN));
        assertFalse(nearlyFull.canAdd(Purse.of(EMERALD_SUN, 2)));
        assertThrows(ArithmeticException.class, () -> nearlyFull.add(Purse.of(EMERALD_SUN, 2)));
    }

    @Test
    void subtractTakesOnlyCoinsThatAreThere() {
        Purse p = Purse.of(GOLD_MARK, 3).with(COPPER_BIT, 5);

        assertEquals(Purse.of(GOLD_MARK, 1).with(COPPER_BIT, 5), p.subtract(Purse.of(GOLD_MARK, 2)));
        assertFalse(p.contains(Purse.of(SILVER_BIT, 1)));
        assertThrows(IllegalArgumentException.class, () -> p.subtract(Purse.of(SILVER_BIT, 1)));
    }

    @Test
    void highestIsTheMostValuableDenominationPresent() {
        assertEquals(NONE, Purse.EMPTY.highest());
        assertEquals(GOLD_REGAL, Purse.of(COPPER_BIT, 900).with(GOLD_REGAL, 1).highest());
    }

    @Test
    void noneIsNeverHeld() {
        assertEquals(Purse.EMPTY, Purse.of(NONE, 5));
        assertEquals(0, Purse.EMPTY.add(NONE, 5).moved());
    }

    @Test
    void negativeCountsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> Purse.of(COPPER_BIT, -1));
    }

    @Test
    void pursesAreValues() {
        assertEquals(Purse.of(GOLD_MARK, 3), Purse.EMPTY.add(GOLD_MARK, 3).updated());
        assertEquals(Purse.of(GOLD_MARK, 3).hashCode(), Purse.EMPTY.add(GOLD_MARK, 3).updated().hashCode());
        assertNotEquals(Purse.of(GOLD_MARK, 3), Purse.of(GOLD_MARK, 4));
    }

    /** The recipes and the tooltips both rely on these exact ratios. */
    @Test
    void denominationValuesAreTheDocumentedOnes() {
        int[] expected = {1, 5, 20, 100, 500, 2500, 20000, 100000};
        var coins = CoinDenomination.coins();
        assertEquals(expected.length, coins.size());
        for (int i = 0; i < expected.length; i++) assertEquals(expected[i], coins.get(i).value, coins.get(i).name());
    }
}
