package com.errantmc.markets.domain;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyFormatTest {

    @Test
    void copperBitsReadAsHundredths() {
        assertEquals("$0.00", MoneyFormat.format(0));
        assertEquals("$0.01", MoneyFormat.format(1));
        assertEquals("$3.02", MoneyFormat.format(302));
        assertEquals("$1,234.56", MoneyFormat.format(123456));
    }

    /** The old formatter followed the JVM locale, so a German client read euros with a comma. */
    @Test
    void theMachinesLocaleChangesNothing() {
        Locale before = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            assertEquals("$1,234.56", MoneyFormat.format(123456));
        } finally {
            Locale.setDefault(before);
        }
    }
}
