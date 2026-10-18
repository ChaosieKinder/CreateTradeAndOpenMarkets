package com.errantmc.markets.item.wallet;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

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

    private static ItemStack findInCurios(Player player) {
        if (!ModList.get().isLoaded("curios")) {
            return ItemStack.EMPTY;
        }

        return CuriosApi.getCuriosInventory(player)
                .map(WalletFinder::scanCurios)
                .orElse(ItemStack.EMPTY);
    }

    private static ItemStack scanCurios(ICuriosItemHandler curios) {
        for (var entry : curios.getCurios().entrySet()) {
            IDynamicStackHandler stacks = entry.getValue().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof WalletItem) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }
}
