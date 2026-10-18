package com.errantmc.markets.data;

public enum CoinDenomination {
    NONE("", 0, ""),                //
    COPPER_BIT("coin_copper_bit", 1, "§4"),            //
    COPPER_SHARD("coin_copper_shard", 5, "§c"),            //
    SILVER_BIT("coin_silver_bit", 20, "§8"),           //
    SILVER_CROWN("coin_silver_crown", 100, "§7"),            //
    GOLD_MARK("coin_gold_mark", 500, "§e"),
    GOLD_REGAL("coin_gold_regal", 2500, "§6"),
    EMERALD_SUN("coin_emerald_sun", 20000, "§2"),
    EMERALD_THRONE("coin_emerald_throne", 100000, "§a");

    public final int value;       // coin worth per 1 item
    public final String format;   // color code (or use TextColor)
    public final String languageKey;

    CoinDenomination(String langKey, int value, String format) {
        this.value = value;
        this.format = format;
        this.languageKey = langKey;
    }
}