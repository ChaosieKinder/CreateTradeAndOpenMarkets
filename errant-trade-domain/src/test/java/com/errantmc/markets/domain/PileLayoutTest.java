package com.errantmc.markets.domain;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.errantmc.markets.domain.CoinDenomination.*;
import static org.junit.jupiter.api.Assertions.*;

class PileLayoutTest {

    @Test
    void anEmptyPileHasNoColumns() {
        assertTrue(PileLayout.of(Purse.EMPTY).isEmpty());
    }

    @Test
    void oneCoinStandsAloneInTheCentre() {
        List<PileLayout.Column> columns = PileLayout.of(Purse.of(GOLD_MARK, 1));

        assertEquals(1, columns.size());
        assertEquals(new PileLayout.Column(GOLD_MARK, 1, 1, 0f, 0f), columns.getFirst());
    }

    @Test
    void coinsStackTenHigh() {
        List<PileLayout.Column> columns = PileLayout.of(Purse.of(COPPER_BIT, 25));

        assertEquals(List.of(10, 10, 5), columns.stream().map(PileLayout.Column::height).toList());
    }

    @Test
    void theMostValuableCoinsTakeTheCentre() {
        Purse purse = Purse.of(COPPER_BIT, 30).with(EMERALD_THRONE, 2).with(SILVER_BIT, 4);

        List<PileLayout.Column> columns = PileLayout.of(purse);

        assertEquals(List.of(EMERALD_THRONE, SILVER_BIT, COPPER_BIT, COPPER_BIT, COPPER_BIT),
                columns.stream().map(PileLayout.Column::denomination).toList());
        assertEquals(0f, columns.getFirst().x());
        assertEquals(0f, columns.getFirst().z());
    }

    /** Whatever the balance, the drawing stays inside the block and no two columns share a place. */
    @Test
    void everyColumnHasItsOwnPlaceInsideTheBlock() {
        for (int copper : new int[]{1, 99, 400, 999, 5000}) {
            Purse purse = Purse.EMPTY;
            for (CoinDenomination d : CoinDenomination.coins()) purse = purse.with(d, copper);

            List<PileLayout.Column> columns = PileLayout.of(purse);

            Set<String> places = new HashSet<>();
            for (PileLayout.Column c : columns) {
                assertTrue(Math.hypot(c.x(), c.z()) <= 0.44 + 1e-6, "column outside the block at " + copper);
                assertTrue(places.add(c.x() + "," + c.z()), "two columns share a place at " + copper);
            }
        }
    }

    /** Folding a tall column changes how it is drawn, never how many coins it stands for. */
    @Test
    void foldedColumnsStillStandForEveryCoin() {
        Purse purse = Purse.EMPTY;
        for (CoinDenomination d : CoinDenomination.coins()) purse = purse.with(d, 400);

        for (PileLayout.Column c : PileLayout.of(purse)) {
            assertTrue(c.height() >= 1 && c.thickness() >= 1);
        }
        long drawn = PileLayout.of(purse).stream()
                .filter(c -> c.denomination() == COPPER_BIT)
                .mapToLong(c -> (long) c.height() * c.thickness()).sum();
        assertEquals(400, drawn);
    }

    @Test
    void aHugeDenominationIsOneTallColumn() {
        List<PileLayout.Column> columns = PileLayout.of(Purse.of(COPPER_BIT, 21 * 64));

        assertEquals(1, columns.size());
        assertEquals(50, columns.getFirst().height());
    }
}
