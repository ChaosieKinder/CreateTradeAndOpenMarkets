package com.errantmc.markets.domain;

import org.junit.jupiter.api.Test;

import static com.errantmc.markets.domain.CoinDenomination.*;
import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {

    @Test
    void exactLooseCoinsPayWithNoChange() {
        Payment p = Payment.plan(Purse.of(SILVER_CROWN, 1), Purse.EMPTY, 100).orElseThrow();

        assertEquals(Purse.of(SILVER_CROWN, 1), p.fromLoose());
        assertEquals(Purse.EMPTY, p.fromWallet());
        assertEquals(Purse.EMPTY, p.change());
    }

    @Test
    void looseCoinsGoBeforeTheWallet() {
        Payment p = Payment.plan(Purse.of(COPPER_SHARD, 4), Purse.of(SILVER_CROWN, 3), 100).orElseThrow();

        assertEquals(Purse.of(COPPER_SHARD, 4), p.fromLoose());
        assertEquals(Purse.of(SILVER_CROWN, 1), p.fromWallet());
        assertEquals(20, p.change().totalValue());
    }

    @Test
    void theWalletAloneCanPay() {
        Payment p = Payment.plan(Purse.EMPTY, Purse.of(GOLD_MARK, 2), 250).orElseThrow();

        assertEquals(Purse.of(GOLD_MARK, 1), p.fromWallet());
        assertEquals(Purse.of(GOLD_MARK, 1), Purse.of(GOLD_MARK, 2).remove(GOLD_MARK, 1).updated());
        assertEquals(250, p.change().totalValue());
    }

    @Test
    void smallCoinsAreSpentBeforeBreakingABigOne() {
        Purse loose = Purse.of(COPPER_BIT, 30).with(GOLD_MARK, 1);

        Payment p = Payment.plan(loose, Purse.EMPTY, 25).orElseThrow();

        assertEquals(Purse.of(COPPER_BIT, 25), p.fromLoose());
        assertEquals(Purse.EMPTY, p.change());
    }

    @Test
    void notEnoughMoneyIsRefused() {
        assertTrue(Payment.plan(Purse.of(COPPER_BIT, 40), Purse.of(COPPER_BIT, 59), 100).isEmpty());
    }

    /** Whatever is paid, coins are conserved: paid minus change is exactly the price. */
    @Test
    void paymentsAlwaysBalance() {
        Purse loose = Purse.of(COPPER_BIT, 7).with(SILVER_BIT, 3).with(GOLD_MARK, 1);
        Purse wallet = Purse.of(COPPER_SHARD, 9).with(GOLD_REGAL, 2).with(EMERALD_SUN, 1);
        for (long price : new long[]{1, 7, 8, 99, 100, 567, 2500, 3333, 20000, 25000}) {
            Payment p = Payment.plan(loose, wallet, price).orElseThrow(() -> new AssertionError("could not pay " + price));
            assertEquals(price, p.paid() - p.change().totalValue(), "price " + price);
            assertTrue(fits(p.fromLoose(), loose), "took more loose coins than there were at " + price);
            assertTrue(fits(p.fromWallet(), wallet), "took more from the wallet than it held at " + price);
        }
    }

    @Test
    void changeUsesTheFewestCoins() {
        assertEquals(Purse.of(GOLD_REGAL, 1).with(SILVER_CROWN, 2).with(COPPER_SHARD, 1).with(COPPER_BIT, 3),
                Payment.change(2708));
        assertEquals(Purse.EMPTY, Payment.change(0));
    }

    @Test
    void aFreeItemCostsNothing() {
        assertEquals(Payment.FREE, Payment.plan(Purse.EMPTY, Purse.EMPTY, 0).orElseThrow());
    }

    private static boolean fits(Purse taken, Purse from) {
        for (CoinDenomination d : CoinDenomination.coins()) if (taken.count(d) > from.count(d)) return false;
        return true;
    }
}
