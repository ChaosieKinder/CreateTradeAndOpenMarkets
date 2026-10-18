package com.errantmc.markets.domain;

import java.util.Arrays;
import java.util.List;

/**
 * The coins, smallest first. {@link #value} is what one coin is worth in copper bits, the unit
 * every total is counted in; display divides by 100 ({@link MoneyFormat}).
 */
public enum CoinDenomination {
    NONE("", 0),
    COPPER_BIT("coin_copper_bit", 1),
    COPPER_SHARD("coin_copper_shard", 5),
    SILVER_BIT("coin_silver_bit", 20),
    SILVER_CROWN("coin_silver_crown", 100),
    GOLD_MARK("coin_gold_mark", 500),
    GOLD_REGAL("coin_gold_regal", 2500),
    EMERALD_SUN("coin_emerald_sun", 20000),
    EMERALD_THRONE("coin_emerald_throne", 100000);

    private static final List<CoinDenomination> COINS =
            Arrays.stream(values()).filter(d -> d != NONE).toList();

    /** The coin's item id, without namespace; also the stem of its translation keys. */
    public final String id;
    /** Worth of one coin, in copper bits. */
    public final int value;

    CoinDenomination(String id, int value) {
        this.id = id;
        this.value = value;
    }

    /** Every real denomination, smallest first: everything but {@link #NONE}. */
    public static List<CoinDenomination> coins() {
        return COINS;
    }
}
