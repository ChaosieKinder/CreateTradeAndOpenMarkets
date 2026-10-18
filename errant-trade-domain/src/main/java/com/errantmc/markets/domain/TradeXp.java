package com.errantmc.markets.domain;

/**
 * Villager experience earned by trading, scaled by the coin value of the trade rather than one
 * fixed amount per trade (author's decision, 18 Oct): a big sale counts for more than a small one,
 * and many small ones add up to the same as one big one. What does not make a whole point is
 * carried to the villager's next trade.
 *
 * @param xp        whole experience points to award now
 * @param remainder copper bits carried towards the next point
 */
public record TradeXp(int xp, long remainder) {

    /**
     * @param tradeValue  the trade's worth in copper bits
     * @param carried     the remainder left by the villager's previous trade
     * @param bitsPerXp   copper bits of trade per point of experience
     */
    public static TradeXp award(long tradeValue, long carried, int bitsPerXp) {
        if (bitsPerXp <= 0) throw new IllegalArgumentException("bitsPerXp must be positive, was " + bitsPerXp);
        long total = Math.max(0, carried) + Math.max(0, tradeValue);
        return new TradeXp((int) Math.min(Integer.MAX_VALUE, total / bitsPerXp), total % bitsPerXp);
    }
}
