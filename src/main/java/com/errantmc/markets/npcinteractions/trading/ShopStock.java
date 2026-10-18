package com.errantmc.markets.npcinteractions.trading;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * What remains of a villager's defined shop today, saved with the villager. A new day restocks:
 * stock for any other day is ignored.
 *
 * @param day       the day it was recorded, in days since the world began
 * @param remaining stock by entry key ({@link #key})
 */
public record ShopStock(long day, Map<String, Integer> remaining) {
    public static final ShopStock NONE = new ShopStock(Long.MIN_VALUE, Map.of());

    public static final Codec<ShopStock> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.LONG.fieldOf("day").forGetter(ShopStock::day),
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("remaining").forGetter(ShopStock::remaining)
    ).apply(inst, ShopStock::new));

    /**
     * An entry's key: its tab, its row, and the item, so that a changed shop definition does not
     * hand one item's stock to another.
     */
    public static String key(int tab, int entry, ItemStack item) {
        return tab + "/" + entry + "/" + BuiltInRegistries.ITEM.getKey(item.getItem());
    }
}
