package com.errantmc.markets.npcinteractions.trading;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Represents a stock entry within a npcinteractions tabs.
 */
public class ShopEntry {
    public static final int UNLIMITED_STOCK = -1;

    private final ItemStack item;
    private final int price;
    private int stock;

    public ShopEntry(ItemStack item, int price, int stock) {
        this.item = item.copy();
        this.price = price;
        this.stock = stock;
    }

    public ShopEntry(RegistryFriendlyByteBuf buffer) {
        this.item = ItemStack.STREAM_CODEC.decode(buffer);
        this.price = buffer.readInt();
        this.stock = buffer.readInt();
    }

    public void writeToStream(RegistryFriendlyByteBuf buffer) {
        ItemStack.STREAM_CODEC.encode(buffer, this.item);
        buffer.writeInt(this.price);
        buffer.writeInt(this.stock);
    }

    public ItemStack item() {
        return this.item;
    }

    public int price() {
        return this.price;
    }

    public int stock() {
        return this.stock;
    }

    public boolean hasLimitedStock() {
        return this.stock != UNLIMITED_STOCK;
    }

    public void increaseStock(int amount) {
        if (amount <= 0 || this.stock == UNLIMITED_STOCK) {
            return;
        }
        this.stock += amount;
    }

    public void decreaseStock(int amount) {
        if (amount <= 0 || this.stock == UNLIMITED_STOCK) {
            return;
        }
        this.stock = Math.max(0, this.stock - amount);
    }
}
