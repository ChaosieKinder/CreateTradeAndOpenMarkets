package com.errantmc.markets.npcinteractions.tabs.buy;

import com.errantmc.markets.npcinteractions.components.SimpleShopTab;
import com.errantmc.markets.npcinteractions.trading.ShopEntry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Simple extension that exposes helper lookups for buy tabs so other tabs
 * types (like sells) can interact with their inventory data.
 */
public class BuyShopTab extends SimpleShopTab {
    public BuyShopTab(ResourceLocation type, Component title, List<ShopEntry> entries) {
        super(type, title, entries);
    }

    public BuyShopTab(ResourceLocation type, RegistryFriendlyByteBuf buf) {
        super(type, buf);
    }

    public Optional<ShopEntry> findEntry(ItemStack stack) {
        for (ShopEntry entry : entries()) {
            if (ItemStack.isSameItemSameComponents(entry.item(), stack)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    @Override
    public void writeToStream(RegistryFriendlyByteBuf buffer) {
        super.writeToStream(buffer);
    }

    @Override
    protected String priceKey() {
        return "screen.erranttrade.npcinteractions.price";
    }
}
