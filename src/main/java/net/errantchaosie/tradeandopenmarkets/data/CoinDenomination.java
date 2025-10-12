package net.errantchaosie.tradeandopenmarkets.data;

public enum CoinDenomination {
    NONE(0, ""),                //
    COPPER(1, "§6"),            //
    BRONZE(5, "§4"),            //
    SILVER(40, "§8"),           //
    GOLD(200, "§e"),            //
    ELECTRUM(1000, "§f"),
    PLATINUM(5000, "§7"),
    EMERALD(32768, "§a");

    public final int value;       // coin worth per 1 item
    public final String format;   // color code (or use TextColor)

    CoinDenomination(int value, String format) {
        this.value = value;
        this.format = format;
    }
}