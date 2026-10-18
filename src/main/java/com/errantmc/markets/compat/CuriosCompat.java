package com.errantmc.markets.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * The only class that may import Curios, which is {@code compileOnly}. Callers go through
 * {@link #isLoaded()} first, so the JVM never links a Curios class when the mod is absent.
 */
public final class CuriosCompat {
    private CuriosCompat() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded("curios");
    }

    /** Every non-empty stack the player wears as a curio. Call only when {@link #isLoaded()}. */
    public static List<ItemStack> allCurios(Player player) {
        List<ItemStack> out = new ArrayList<>();
        CuriosApi.getCuriosInventory(player).ifPresent(inv -> {
            for (var handler : inv.getCurios().values()) {
                var stacks = handler.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    if (!stacks.getStackInSlot(i).isEmpty()) out.add(stacks.getStackInSlot(i));
                }
            }
        });
        return out;
    }

    /** The first curio slot holding a matching stack, or null. Call only when {@link #isLoaded()}. */
    public static @Nullable ItemStack findCurio(Player player, Predicate<ItemStack> matches) {
        var inv = CuriosApi.getCuriosInventory(player);
        if (inv.isEmpty()) return null;
        for (var handler : inv.get().getCurios().values()) {
            var stacks = handler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack s = stacks.getStackInSlot(i);
                if (!s.isEmpty() && matches.test(s)) return s;
            }
        }
        return null;
    }
}
