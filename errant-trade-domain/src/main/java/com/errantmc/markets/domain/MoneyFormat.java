package com.errantmc.markets.domain;

import java.util.Locale;

/**
 * How an amount of money reads, the same for every player whatever their machine's locale.
 * One place, so the currency symbol is a one-line change.
 */
public final class MoneyFormat {
    private MoneyFormat() {}

    /** {@code 123456} copper bits reads {@code $1,234.56}. */
    public static String format(long copperBits) {
        String sign = copperBits < 0 ? "-" : "";
        long abs = Math.abs(copperBits);
        return String.format(Locale.ROOT, "%s$%,d.%02d", sign, abs / 100, abs % 100);
    }
}
