package com.errantmc.markets.item.wallet;

import com.errantmc.markets.compat.CuriosCompat;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Utility helpers for locating wallet stacks across the player's equipment, inventory, and curios slots.
 */
public final class WalletFinder {
    private WalletFinder() {}

    /**
     * Attempts to locate the first wallet stack carried by the given player.
     *
     * @param player player to search; may be {@code null}
     * @return the first located wallet stack, or {@link ItemStack#EMPTY} when no wallet is present
     */
    public static ItemStack findWallet(@Nullable Player player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }

        ItemStack wallet = findInHands(player);
        if (!wallet.isEmpty()) {
            return wallet;
        }

        wallet = findInInventory(player);
        if (!wallet.isEmpty()) {
            return wallet;
        }

        return findInCurios(player);
    }

    private static ItemStack findInHands(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof WalletItem) {
            return main;
        }

        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof WalletItem) {
            return offhand;
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack findInInventory(Player player) {
        NonNullList<ItemStack> items = player.getInventory().items;
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && stack.getItem() instanceof WalletItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Curios is optional; only CuriosCompat may name it (AGENTS §3). */
    private static ItemStack findInCurios(Player player) {
        if (!CuriosCompat.isLoaded()) {
            return ItemStack.EMPTY;
        }
        ItemStack worn = CuriosCompat.findCurio(player, stack -> stack.getItem() instanceof WalletItem);
        return worn != null ? worn : ItemStack.EMPTY;
    }
}
