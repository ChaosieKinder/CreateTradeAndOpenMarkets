package com.errantmc.markets.item;

import com.errantmc.markets.component.Purses;
import com.errantmc.markets.domain.Purse;
import net.minecraft.world.item.ItemStack;

/**
 * Putting coins from an item stack into a purse: the one routine the coin pile, the wallet screen
 * and wallet pickup all use. Four copies of this rule are how bugs 1–3 happened.
 */
public final class CoinDeposits {
    private CoinDeposits() {}

    /**
     * Moves what fits of {@code stack} into {@code purse}, shrinking the stack by what went in
     * unless {@code consume} is false (creative). Coins go in as far as they fit; a coin stack
     * goes in whole or not at all. Anything else is left alone.
     * @return the purse afterwards
     */
    public static Purse absorb(Purse purse, ItemStack stack, boolean consume) {
        if (stack.getItem() instanceof CoinItem coin) {
            var change = purse.add(coin.denom(), stack.getCount());
            if (consume) stack.shrink(change.moved());
            return change.updated();
        }
        if (stack.getItem() instanceof CoinStackItem) {
            Purse inside = Purses.get(stack);
            int stacks = consume ? stack.getCount() : 1;
            for (int i = 0; i < stacks && purse.canAdd(inside); i++) {
                purse = purse.add(inside);
                if (consume) stack.shrink(1);
            }
        }
        return purse;
    }

    public static Purse absorb(Purse purse, ItemStack stack) {
        return absorb(purse, stack, true);
    }
}
