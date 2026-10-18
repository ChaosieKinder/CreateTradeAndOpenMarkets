package com.errantmc.markets.domain;

import java.util.Arrays;
import java.util.StringJoiner;

/**
 * An amount of coins, counted per denomination: what a wallet, a coin pile or a coin stack holds.
 * Immutable. No denomination ever holds more than {@code Integer.MAX_VALUE}.
 *
 * <p>Coins are conserved: every operation either moves exactly what it reports or refuses.
 * {@link #add(CoinDenomination, int)} takes what fits and reports the rest; a whole purse is merged
 * only after {@link #canAdd} says it fits.
 */
public final class Purse {
    public static final Purse EMPTY = new Purse(new int[CoinDenomination.values().length]);

    private final int[] counts; // indexed by ordinal; NONE's slot is always 0

    private Purse(int[] counts) {
        this.counts = counts;
    }

    public static Purse of(CoinDenomination denomination, int count) {
        return EMPTY.with(denomination, count);
    }

    public int count(CoinDenomination denomination) {
        return counts[denomination.ordinal()];
    }

    /** This purse with {@code denomination} set to exactly {@code count}. */
    public Purse with(CoinDenomination denomination, int count) {
        if (count < 0) throw new IllegalArgumentException("negative count " + count + " of " + denomination);
        if (denomination == CoinDenomination.NONE) return this;
        int[] next = counts.clone();
        next[denomination.ordinal()] = count;
        return new Purse(next);
    }

    /** Adds what fits under {@code Integer.MAX_VALUE}; the rest is reported, never lost. */
    public Change add(CoinDenomination denomination, int count) {
        if (denomination == CoinDenomination.NONE || count <= 0) return new Change(this, denomination, 0, Math.max(count, 0));
        int current = count(denomination);
        int moved = Math.min(count, Integer.MAX_VALUE - current);
        return new Change(with(denomination, current + moved), denomination, moved, count - moved);
    }

    /** Removes what is there, up to {@code count}; the shortfall is reported. */
    public Change remove(CoinDenomination denomination, int count) {
        if (denomination == CoinDenomination.NONE || count <= 0) return new Change(this, denomination, 0, Math.max(count, 0));
        int current = count(denomination);
        int moved = Math.min(count, current);
        return new Change(with(denomination, current - moved), denomination, moved, count - moved);
    }

    /** Whether {@link #add(Purse)} would fit: no denomination may pass {@code Integer.MAX_VALUE}. */
    public boolean canAdd(Purse other) {
        for (CoinDenomination d : CoinDenomination.coins()) {
            if ((long) count(d) + other.count(d) > Integer.MAX_VALUE) return false;
        }
        return true;
    }

    /**
     * Merges two purses. Callers check {@link #canAdd} first and refuse the merge when it fails;
     * an overflow here throws rather than lose or wrap coins.
     */
    public Purse add(Purse other) {
        int[] next = counts.clone();
        for (CoinDenomination d : CoinDenomination.coins()) {
            next[d.ordinal()] = Math.addExact(next[d.ordinal()], other.count(d));
        }
        return new Purse(next);
    }

    /** Total worth in copper bits. Cannot overflow: at most 8 × 2³¹ × 100,000. */
    public long totalValue() {
        long total = 0;
        for (CoinDenomination d : CoinDenomination.coins()) total += (long) count(d) * d.value;
        return total;
    }

    public long totalCount() {
        long total = 0;
        for (int c : counts) total += c;
        return total;
    }

    /** The most valuable denomination present, or {@link CoinDenomination#NONE} when empty. */
    public CoinDenomination highest() {
        var coins = CoinDenomination.coins();
        for (int i = coins.size() - 1; i >= 0; i--) {
            if (count(coins.get(i)) > 0) return coins.get(i);
        }
        return CoinDenomination.NONE;
    }

    public boolean isEmpty() {
        return totalCount() == 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Purse p && Arrays.equals(counts, p.counts);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(counts);
    }

    @Override
    public String toString() {
        StringJoiner out = new StringJoiner(", ", "Purse{", "}");
        for (CoinDenomination d : CoinDenomination.coins()) {
            if (count(d) > 0) out.add(d + "=" + count(d));
        }
        return out.toString();
    }

    /**
     * The result of adding or removing one denomination.
     * @param updated   the purse afterwards
     * @param moved     how many coins actually went in or out
     * @param remaining how many of those asked for did not
     */
    public record Change(Purse updated, CoinDenomination denomination, int moved, int remaining) {}
}
