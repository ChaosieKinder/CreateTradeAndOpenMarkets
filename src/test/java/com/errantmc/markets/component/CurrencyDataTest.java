package com.errantmc.markets.component;

import com.errantmc.markets.data.CoinDenomination;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurrencyDataTest {

    @Test
    void totalValueIsCountedInCopperBitsAndShownInHundredths() {
        CurrencyData c = CurrencyData.EMPTY
                .add(CoinDenomination.SILVER_CROWN, 3).updated()
                .add(CoinDenomination.COPPER_BIT, 2).updated();

        assertEquals(3.02, c.totalValue(), 1e-9);
    }

    @Test
    void removeTakesOnlyWhatIsThereAndReportsTheRest() {
        CurrencyData c = CurrencyData.newCurrencyFromDenom(CoinDenomination.GOLD_MARK, 2);

        var result = c.remove(CoinDenomination.GOLD_MARK, 5);

        assertEquals(2, result.delta());
        assertEquals(3, result.remaining());
        assertEquals(0, result.updated().getFrom(CoinDenomination.GOLD_MARK));
    }

    /** BACKLOG #4: the int sum used to wrap before widening. */
    @Test
    void totalCountDoesNotOverflow() {
        CurrencyData c = CurrencyData.newCurrencyFromDenom(CoinDenomination.COPPER_BIT, Integer.MAX_VALUE)
                .add(CoinDenomination.COPPER_SHARD, Integer.MAX_VALUE).updated();

        assertEquals(2L * Integer.MAX_VALUE, c.totalCount());
    }

    @Test
    void addClampsAtIntMaxAndReportsTheRest() {
        CurrencyData c = CurrencyData.newCurrencyFromDenom(CoinDenomination.COPPER_BIT, Integer.MAX_VALUE - 1);

        var result = c.add(CoinDenomination.COPPER_BIT, 5);

        assertEquals(1, result.delta());
        assertEquals(4, result.remaining());
        assertEquals(Integer.MAX_VALUE, result.updated().getFrom(CoinDenomination.COPPER_BIT));
    }
}
