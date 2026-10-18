package com.errantmc.markets.npcinteractions.trading;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/**
 * One line of a shop: on a buy tab, what the player gets for {@code price}; on a sell tab, what the
 * player hands over for it. Prices are in copper bits.
 */
public class ShopEntry {
    public static final int UNLIMITED_STOCK = -1;
    /** Not converted from a vanilla offer. */
    public static final int NO_OFFER = -1;

    private final ItemStack item;
    /** Set again by dynamic pricing each time the shop is resolved (InteractionTabManager). */
    private int price;
    private int stock;
    /** Items the player must also hand over, as a vanilla trade's second cost (the librarian's book). */
    private final ItemStack extraCost;
    /** Index into the villager's vanilla offers this entry was converted from, or {@link #NO_OFFER}. */
    private final int offerIndex;

    public ShopEntry(ItemStack item, int price, int stock) {
        this(item, price, stock, ItemStack.EMPTY, NO_OFFER);
    }

    public ShopEntry(ItemStack item, int price, int stock, ItemStack extraCost, int offerIndex) {
        this.item = item.copy();
        this.price = price;
        this.stock = stock;
        this.extraCost = extraCost.copy();
        this.offerIndex = offerIndex;
    }

    public ShopEntry(RegistryFriendlyByteBuf buffer) {
        this.item = ItemStack.STREAM_CODEC.decode(buffer);
        this.price = buffer.readInt();
        this.stock = buffer.readInt();
        this.extraCost = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        this.offerIndex = buffer.readVarInt();
    }

    public void writeToStream(RegistryFriendlyByteBuf buffer) {
        ItemStack.STREAM_CODEC.encode(buffer, this.item);
        buffer.writeInt(this.price);
        buffer.writeInt(this.stock);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, this.extraCost);
        buffer.writeVarInt(this.offerIndex);
    }

    public ItemStack item() {
        return this.item;
    }

    /** In copper bits. */
    public int price() {
        return this.price;
    }

    public void setPrice(int price) {
        this.price = Math.max(0, price);
    }

    public int stock() {
        return this.stock;
    }

    public ItemStack extraCost() {
        return this.extraCost;
    }

    public int offerIndex() {
        return this.offerIndex;
    }

    public boolean fromVanillaOffer() {
        return this.offerIndex != NO_OFFER;
    }

    public boolean hasLimitedStock() {
        return this.stock != UNLIMITED_STOCK;
    }

    public boolean inStock() {
        return !hasLimitedStock() || this.stock > 0;
    }

    public void setStock(int stock) {
        this.stock = stock;
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
