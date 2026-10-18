package com.errantmc.markets.item;

import com.errantmc.markets.component.Purses;
import com.errantmc.markets.domain.CoinDenomination;
import com.errantmc.markets.domain.Purse;
import com.errantmc.markets.item.wallet.WalletFinder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A player's money as the shops see it: loose coins in the inventory, and the first wallet they
 * carry. Spending takes loose coins first, then the wallet (author's decision, 18 Oct); income
 * and change go into the wallet if one is carried, otherwise into the inventory as coins.
 */
public final class PlayerMoney {
    private PlayerMoney() {}

    /** Loose coins in the main inventory, hotbar and offhand. */
    public static Purse loose(Player player) {
        Purse purse = Purse.EMPTY;
        for (ItemStack s : carried(player)) {
            if (s.getItem() instanceof CoinItem coin) purse = purse.add(coin.denom(), s.getCount()).updated();
        }
        return purse;
    }

    /** The carried wallet's balance, or nothing without a wallet. */
    public static Purse wallet(Player player) {
        ItemStack wallet = WalletFinder.findWallet(player);
        return wallet.isEmpty() ? Purse.EMPTY : Purses.get(wallet);
    }

    /** Removes exactly {@code coins} from the inventory. The caller has checked they are there. */
    public static void takeLoose(Player player, Purse coins) {
        for (CoinDenomination d : CoinDenomination.coins()) {
            int due = coins.count(d);
            for (ItemStack s : carried(player)) {
                if (due == 0) break;
                if (s.getItem() instanceof CoinItem coin && coin.denom() == d) {
                    int n = Math.min(due, s.getCount());
                    s.shrink(n);
                    due -= n;
                }
            }
            if (due > 0) throw new IllegalStateException("inventory was short of " + due + " " + d);
        }
    }

    /** Removes exactly {@code coins} from the carried wallet. The caller has checked they are there. */
    public static void takeFromWallet(Player player, Purse coins) {
        if (coins.isEmpty()) return;
        ItemStack wallet = WalletFinder.findWallet(player);
        Purses.set(wallet, Purses.get(wallet).subtract(coins));
    }

    /**
     * Pays {@code money} to the player: into the wallet if one is carried, as far as it fits, and
     * the rest as coins in the inventory, dropped at the player's feet when that is full.
     */
    public static void give(Player player, Purse money) {
        ItemStack wallet = WalletFinder.findWallet(player);
        for (CoinDenomination d : CoinDenomination.coins()) {
            int count = money.count(d);
            if (count == 0) continue;
            if (!wallet.isEmpty()) count = Purses.add(wallet, d, count).remaining();
            while (count > 0) {
                ItemStack coins = CoinItem.createCurrencyItem(d, Math.min(count, CoinItem.forDenomination(d).getDefaultMaxStackSize()));
                count -= coins.getCount();
                giveItem(player, coins);
            }
        }
    }

    /**
     * The money in {@code stack} itself, in copper bits: coins, or the purse of a coin stack or a
     * wallet. Zero for anything else. A shop selling coins trades money for money, and its till
     * must account for that.
     */
    public static long worth(ItemStack stack) {
        long each = stack.getItem() instanceof CoinItem coin ? coin.denom().value : Purses.get(stack).totalValue();
        return each * stack.getCount();
    }

    /** Into the inventory, or dropped at the player's feet if it does not fit. */
    public static void giveItem(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack) && !stack.isEmpty()) player.drop(stack, false);
    }

    private static List<ItemStack> carried(Player player) {
        List<ItemStack> out = new ArrayList<>(player.getInventory().items);
        out.addAll(player.getInventory().offhand);
        return out;
    }
}
