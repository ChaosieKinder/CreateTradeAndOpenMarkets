package net.errantchaosie.tradeandopenmarkets.shop.tab;

import net.minecraft.world.item.ItemStack;

/**
 * Represents a stock entry within a shop tab.
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
