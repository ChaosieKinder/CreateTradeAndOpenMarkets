package net.errantchaosie.tradeandopenmarkets.shop.tab;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Simple extension that exposes helper lookups for buy tabs so other tab
 * types (like sells) can interact with their inventory data.
 */
public class BuyShopTab extends SimpleShopTab {
    public BuyShopTab(ResourceLocation type, Component title, List<ShopEntry> entries) {
        super(type, title, entries);
    }

    public Optional<ShopEntry> findEntry(ItemStack stack) {
        for (ShopEntry entry : entries()) {
            if (ItemStack.isSameItemSameComponents(entry.item(), stack)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }
}
