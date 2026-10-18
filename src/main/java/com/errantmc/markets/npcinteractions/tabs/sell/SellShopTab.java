package com.errantmc.markets.npcinteractions.tabs.sell;

import com.errantmc.markets.npcinteractions.components.SimpleShopTab;
import com.errantmc.markets.npcinteractions.trading.ShopEntry;
import com.errantmc.markets.npcinteractions.tabs.buy.BuyShopTab;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sell tabs variant that can automatically generate buyback entries for every
 * item offered on buy tabs.
 */
public class SellShopTab extends SimpleShopTab {
    private final boolean autoBuybacks;
    private final double buybackRate;
    private final Map<ShopEntry, ShopEntry> buybackLinks = new HashMap<>();

    public SellShopTab(ResourceLocation type, Component title, List<ShopEntry> entries,
                       boolean autoBuybacks, double buybackRate) {
        super(type, title, entries);
        this.autoBuybacks = autoBuybacks;
        this.buybackRate = buybackRate;
    }

    public SellShopTab(ResourceLocation type, RegistryFriendlyByteBuf buf) {
        super(type, buf);
        this.autoBuybacks = buf.readBoolean();
        this.buybackRate = buf.readDouble();
    }

    @Override
    public void writeToStream(RegistryFriendlyByteBuf buffer) {
        super.writeToStream(buffer);
        buffer.writeBoolean(this.autoBuybacks);
        buffer.writeDouble(this.buybackRate);
    }

    public boolean autoBuybacksEnabled() {
        return this.autoBuybacks;
    }

    public double buybackRate() {
        return this.buybackRate;
    }

    public void addBuybacksFrom(BuyShopTab buyTab) {
        if (!this.autoBuybacks || buyTab == null) {
            return;
        }
        List<ShopEntry> additions = new ArrayList<>();
        for (ShopEntry entry : buyTab.entries()) {
            if (hasBuybackFor(entry.item())) {
                continue;
            }
            int price = Math.max(1, Mth.floor(entry.price() * this.buybackRate + 0.5f));
            ShopEntry buybackEntry = new ShopEntry(entry.item(), price, ShopEntry.UNLIMITED_STOCK);
            additions.add(buybackEntry);
            this.buybackLinks.put(buybackEntry, entry);
        }
        if (!additions.isEmpty()) {
            mutableEntries().addAll(additions);
        }
    }

    public void recordBuyback(ItemStack soldStack, int amount) {
        if (amount <= 0) {
            return;
        }
        for (Map.Entry<ShopEntry, ShopEntry> link : this.buybackLinks.entrySet()) {
            if (ItemStack.isSameItemSameComponents(link.getKey().item(), soldStack)) {
                link.getValue().increaseStock(amount);
                break;
            }
        }
    }

    private boolean hasBuybackFor(ItemStack itemStack) {
        for (ShopEntry entry : entries()) {
            if (ItemStack.isSameItemSameComponents(entry.item(), itemStack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected String priceKey() {
        return "screen.erranttrade.npcinteractions.pays";
    }
}
