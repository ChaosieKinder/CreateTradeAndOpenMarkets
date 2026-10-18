package com.errantmc.markets.domain;

/**
 * A villager's days settled at once (docs/design/economy.md §7). Each day, in a village:
 * <ol>
 *   <li>the treasury's income, new money, up to the treasury's ceiling;</li>
 *   <li>the villager's wage, paid only from what the treasury has;</li>
 *   <li>upkeep: a share of the villager's balance above a floor, back to the treasury.</li>
 * </ol>
 * Then, once, the restocking the villager owes for what it sold, paid from its till as far as the
 * till goes and destroyed. A villager in no village earns no wage and pays no upkeep. A banker's
 * till is the treasury, so it earns no wage from it and pays no upkeep into it.
 *
 * @param till      the villager's balance after settling (the treasury's, for a banker)
 * @param treasury  the treasury's balance after settling
 * @param created   new money the treasury's income brought in
 * @param destroyed money spent on restocking
 */
public record DailySettlement(long till, long treasury, long created, long destroyed) {

    /**
     * @param income         the treasury's income per villager per day
     * @param ceiling        the treasury stops earning at this balance
     * @param wage           this villager's daily wage
     * @param upkeepPercent  share of the balance above {@code upkeepFloor} paid each day
     * @param upkeepFloor    a villager keeps this much free of upkeep
     */
    public record Rates(long income, long ceiling, long wage, int upkeepPercent, long upkeepFloor) {
        public Rates {
            if (income < 0 || ceiling < 0 || wage < 0 || upkeepFloor < 0) throw new IllegalArgumentException("negative rate");
            if (upkeepPercent < 0 || upkeepPercent > 100) throw new IllegalArgumentException("upkeepPercent " + upkeepPercent);
        }
    }

    public enum Membership {
        /** In no village: no treasury, no wage, no upkeep. */
        NONE,
        /** An ordinary villager of a village. */
        VILLAGER,
        /** A village's banker, trading from the treasury itself. */
        BANKER
    }

    /**
     * @param till         the villager's balance (ignored for a banker, whose till is the treasury)
     * @param treasury     the treasury's balance (ignored in no village)
     * @param days         whole days to settle, already capped by the caller
     * @param restockOwed  what restocking the goods sold since costs
     */
    public static DailySettlement settle(long till, long treasury, Membership membership, Rates rates, long days, long restockOwed) {
        if (till < 0 || treasury < 0 || days < 0 || restockOwed < 0) throw new IllegalArgumentException("negative input");
        long created = 0;
        if (membership != Membership.NONE) {
            for (long d = 0; d < days; d++) {
                long income = Math.min(rates.income(), Math.max(0, rates.ceiling() - treasury));
                treasury += income;
                created += income;
                if (membership == Membership.VILLAGER) {
                    long wage = Math.min(rates.wage(), treasury);
                    treasury -= wage;
                    till += wage;
                    long upkeep = percent(Math.max(0, till - rates.upkeepFloor()), rates.upkeepPercent());
                    till -= upkeep;
                    treasury += upkeep;
                }
            }
        }
        long destroyed;
        if (membership == Membership.BANKER) {
            destroyed = Math.min(restockOwed, treasury);
            treasury -= destroyed;
        } else {
            destroyed = Math.min(restockOwed, till);
            till -= destroyed;
        }
        return new DailySettlement(till, treasury, created, destroyed);
    }

    /** {@code percent} of {@code amount}, rounded down, without overflowing. */
    public static long percent(long amount, int percent) {
        return amount / 100 * percent + amount % 100 * percent / 100;
    }
}
