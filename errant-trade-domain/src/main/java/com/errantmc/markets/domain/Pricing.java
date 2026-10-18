package com.errantmc.markets.domain;

/**
 * Prices that follow a villager's stock and money (docs/design/economy.md §5.1): supply and demand
 * without a market simulation. A villager that has sold much of the day's stock charges more for
 * the rest; one whose till is running low pays less for what it buys.
 */
public final class Pricing {
    private Pricing() {}

    /**
     * What a villager charges for goods of which it has sold {@code rolled - remaining} today:
     * {@code percentPerSale} more for each, up to {@code maxRisePercent} more. Unlimited stock
     * ({@code rolled <= 0}) never rises.
     */
    public static long scarce(long base, int rolled, int remaining, int percentPerSale, int maxRisePercent) {
        if (base <= 0 || rolled <= 0) return base;
        long sold = Math.max(0, (long) rolled - Math.max(0, remaining));
        long rise = Math.min(Math.max(0, maxRisePercent), sold * Math.max(0, percentPerSale));
        return base + DailySettlement.percent(base, (int) rise);
    }

    /**
     * What a villager pays for goods when its till holds {@code balance}: the full price from
     * {@code comfortable} up, falling in a straight line to {@code floorPercent} of it at an empty
     * till. Never below one copper bit for something with a price.
     */
    public static long shortOfMoney(long base, long balance, long comfortable, int floorPercent) {
        if (base <= 0 || comfortable <= 0 || balance >= comfortable) return base;
        int floor = Math.clamp(floorPercent, 0, 100);
        long share = floor + (long) (100 - floor) * Math.max(0, balance) / comfortable; // below 100 here
        return Math.max(1, DailySettlement.percent(base, (int) share));
    }
}
