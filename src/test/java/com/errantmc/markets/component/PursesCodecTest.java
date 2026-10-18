package com.errantmc.markets.component;

import com.errantmc.markets.domain.CoinDenomination;
import com.errantmc.markets.domain.Purse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The purse codec is a save format (AGENTS §5): wallets, coin stacks and coin piles on disk. */
class PursesCodecTest {

    private static final Set<String> FIELDS = Set.of(
            "coinsCopperBit", "coinsCopperShard", "coinsSilverBit", "coinsSilverCrown",
            "coinsGoldMark", "coinsGoldRegal", "coinsEmeraldSun", "coinsEmeraldThrone");

    @Test
    void itWritesTheFieldNamesExistingSavesUse() {
        Purse purse = Purse.of(CoinDenomination.GOLD_MARK, 7).with(CoinDenomination.COPPER_BIT, 3);

        JsonObject json = Purses.CODEC.encodeStart(JsonOps.INSTANCE, purse).getOrThrow().getAsJsonObject();

        assertEquals(FIELDS, json.keySet());
        assertEquals(7, json.get("coinsGoldMark").getAsInt());
    }

    /** Exactly what the old CurrencyData record wrote. */
    @Test
    void itReadsAnOldSave() {
        var old = JsonParser.parseString("""
                {"coinsCopperBit":3,"coinsCopperShard":0,"coinsSilverBit":0,"coinsSilverCrown":12,
                 "coinsGoldMark":0,"coinsGoldRegal":0,"coinsEmeraldSun":0,"coinsEmeraldThrone":1}""");

        Purse purse = Purses.CODEC.parse(JsonOps.INSTANCE, old).getOrThrow();

        assertEquals(Purse.of(CoinDenomination.COPPER_BIT, 3)
                .with(CoinDenomination.SILVER_CROWN, 12)
                .with(CoinDenomination.EMERALD_THRONE, 1), purse);
    }

    @Test
    void itRoundTrips() {
        Purse purse = Purse.EMPTY;
        int n = 1;
        for (CoinDenomination d : CoinDenomination.coins()) purse = purse.with(d, n++ * 1000);
        var encoded = Purses.CODEC.encodeStart(JsonOps.INSTANCE, purse).getOrThrow();

        assertEquals(purse, Purses.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
    }
}
