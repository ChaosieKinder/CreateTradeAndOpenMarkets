package com.errantmc.markets.domain;

import com.errantmc.markets.domain.DailySettlement.Membership;
import com.errantmc.markets.domain.DailySettlement.Rates;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DailySettlementTest {
    /** Income 100, ceiling 100000, wage 100, upkeep 10% above 2000. */
    private static final Rates RATES = new Rates(100, 100_000, 100, 10, 2000);

    @Test
    void aDayInAVillagePaysIncomeThenTheWage() {
        DailySettlement s = DailySettlement.settle(1000, 5000, Membership.VILLAGER, RATES, 1, 0);
        assertEquals(1100, s.till());
        assertEquals(5000, s.treasury()); // +100 income, -100 wage
        assertEquals(100, s.created());
        assertEquals(0, s.destroyed());
    }

    @Test
    void upkeepIsTakenAboveTheFloorAndGoesToTheTreasury() {
        DailySettlement s = DailySettlement.settle(2900, 5000, Membership.VILLAGER, RATES, 1, 0);
        // 2900 + 100 wage = 3000; 10% of the 1000 above the floor is 100
        assertEquals(2900, s.till());
        assertEquals(5100, s.treasury());
    }

    @Test
    void movementsOtherThanIncomeAndRestockingAreTransfers() {
        DailySettlement s = DailySettlement.settle(4321, 777, Membership.VILLAGER, RATES, 3, 250);
        assertEquals(4321 + 777 + s.created() - s.destroyed(), s.till() + s.treasury());
    }

    @Test
    void aPoorTreasuryPaysShortWages() {
        DailySettlement s = DailySettlement.settle(0, 0, Membership.VILLAGER, new Rates(30, 100_000, 100, 10, 2000), 1, 0);
        assertEquals(30, s.till());
        assertEquals(0, s.treasury());
    }

    @Test
    void theTreasuryStopsEarningAtItsCeiling() {
        DailySettlement s = DailySettlement.settle(0, 99_950, Membership.BANKER, RATES, 2, 0);
        assertEquals(100_000, s.treasury());
        assertEquals(50, s.created());
    }

    @Test
    void aVillagerInNoVillageEarnsNothingButStillPaysToRestock() {
        DailySettlement s = DailySettlement.settle(1000, 0, Membership.NONE, RATES, 5, 50);
        assertEquals(950, s.till());
        assertEquals(0, s.created());
        assertEquals(50, s.destroyed());
    }

    @Test
    void restockingTakesOnlyWhatTheTillHas() {
        DailySettlement s = DailySettlement.settle(20, 0, Membership.NONE, RATES, 0, 50);
        assertEquals(0, s.till());
        assertEquals(20, s.destroyed());
    }

    @Test
    void aBankerEarnsNoWageAndRestocksFromTheTreasury() {
        DailySettlement s = DailySettlement.settle(1234, 5000, Membership.BANKER, RATES, 1, 40);
        assertEquals(1234, s.till());
        assertEquals(5000 + 100 - 40, s.treasury());
    }

    @Test
    void noDaysSettlesOnlyTheRestocking() {
        DailySettlement s = DailySettlement.settle(1000, 5000, Membership.VILLAGER, RATES, 0, 10);
        assertEquals(990, s.till());
        assertEquals(5000, s.treasury());
    }

    @Test
    void percentRoundsDownWithoutOverflow() {
        assertEquals(4, DailySettlement.percent(99, 5));
        assertEquals(Long.MAX_VALUE / 100 * 50 + Long.MAX_VALUE % 100 * 50 / 100, DailySettlement.percent(Long.MAX_VALUE, 50));
        assertEquals(Long.MAX_VALUE, DailySettlement.percent(Long.MAX_VALUE, 100));
    }

    @Test
    void negativeInputsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> DailySettlement.settle(-1, 0, Membership.NONE, RATES, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Rates(0, 0, 0, 101, 0));
    }
}
