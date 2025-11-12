package net.errantchaosie.tradeandopenmarkets.shop;

import net.errantchaosie.tradeandopenmarkets.shop.tab.config.ShopTabDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Represents the configured tabs for a particular merchant profession.
 */
public record ShopDefinition(ResourceLocation profession, List<ShopTabDefinition> tabs) {
    public ShopDefinition {
        tabs = List.copyOf(tabs);
    }
}
