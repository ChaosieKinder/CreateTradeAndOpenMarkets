package com.errantmc.markets.domain;

import java.util.List;
import java.util.Optional;

/**
 * How a price is paid: which coins leave the player's inventory, which leave the wallet, and what
 * change comes back. Loose coins are spent before the wallet (author's decision, 18 Oct), and
 * within each the smallest coins go first, so a purchase clears small change rather than breaking
 * large coins.
 *
 * @param fromLoose  coins taken from the inventory
 * @param fromWallet coins taken from the wallet's balance
 * @param change     what the player gets back; worth exactly what was paid over the price
 */
public record Payment(Purse fromLoose, Purse fromWallet, Purse change) {
    public static final Payment FREE = new Payment(Purse.EMPTY, Purse.EMPTY, Purse.EMPTY);

    /** Plans paying {@code price} copper bits, or empty if the player cannot afford it. */
    public static Optional<Payment> plan(Purse loose, Purse wallet, long price) {
        if (price < 0) throw new IllegalArgumentException("negative price " + price);
        if (price == 0) return Optional.of(FREE);
        if (loose.totalValue() + wallet.totalValue() < price) return Optional.empty();

        Purse fromLoose = take(loose, price);
        long due = price - fromLoose.totalValue();
        Purse fromWallet = due > 0 ? take(wallet, due) : Purse.EMPTY;
        long overpaid = fromLoose.totalValue() + fromWallet.totalValue() - price;
        return Optional.of(new Payment(fromLoose, fromWallet, change(overpaid)));
    }

    /** Coins from {@code source}, smallest first, until they cover {@code due} or run out. */
    private static Purse take(Purse source, long due) {
        Purse taken = Purse.EMPTY;
        for (CoinDenomination d : CoinDenomination.coins()) {
            if (due <= 0) break;
            long needed = (due + d.value - 1) / d.value;
            int n = (int) Math.min(source.count(d), needed);
            if (n == 0) continue;
            taken = taken.with(d, n);
            due -= (long) n * d.value;
        }
        return taken;
    }

    /**
     * {@code copperBits} in the fewest coins. Every denomination divides the next, so taking the
     * largest coin that fits is optimal.
     */
    public static Purse change(long copperBits) {
        if (copperBits < 0) throw new IllegalArgumentException("negative change " + copperBits);
        Purse out = Purse.EMPTY;
        List<CoinDenomination> coins = CoinDenomination.coins();
        for (int i = coins.size() - 1; i >= 0 && copperBits > 0; i--) {
            CoinDenomination d = coins.get(i);
            long n = copperBits / d.value;
            if (n > 0) {
                out = out.with(d, (int) Math.min(n, Integer.MAX_VALUE));
                copperBits -= n * d.value;
            }
        }
        return out;
    }

    /** Total that leaves the player, before change. */
    public long paid() {
        return fromLoose.totalValue() + fromWallet.totalValue();
    }
}
