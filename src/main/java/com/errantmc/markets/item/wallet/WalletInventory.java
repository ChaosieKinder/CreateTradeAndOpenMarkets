package com.errantmc.markets.item.wallet;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

public final class WalletInventory {
    public static final int SIZE = 9;

    private WalletInventory() {}

    /** Ensure the stack has a 9-slot container component initialized to empties. */
    public static void ensure(ItemStack stack) {
        if (stack.get(DataComponents.CONTAINER) == null) {
            List<ItemStack> empty = new ArrayList<>(SIZE);
            for (int i = 0; i < SIZE; i++) empty.add(ItemStack.EMPTY);
            stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(empty));
        }
    }

    /** Read a **copy** of the item currently stored on the stack. Never null. */
    public static List<ItemStack> read(ItemStack stack) {
        ItemContainerContents c = stack.get(DataComponents.CONTAINER);
        List<ItemStack> out = new ArrayList<>(SIZE);
        if (c == null) {
            for (int i = 0; i < SIZE; i++) out.add(ItemStack.EMPTY);
            return out;
        }
        // Most mappings expose c.stacks() (a List<ItemStack>)
        List<ItemStack> src = c.stream().toList(); // if your mapping names it differently, use that getter
        for (int i = 0; i < SIZE; i++) {
            ItemStack s = (i < src.size()) ? src.get(i) : ItemStack.EMPTY;
            out.add(s.copy());
        }
        return out;
    }

    /** Write item back to the stack’s component (persists). */
    public static void write(ItemStack stack, List<ItemStack> items) {
        // item.size() should be SIZE
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }


}