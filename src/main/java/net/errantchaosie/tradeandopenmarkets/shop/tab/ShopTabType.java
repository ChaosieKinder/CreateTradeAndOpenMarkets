package net.errantchaosie.tradeandopenmarkets.shop.tab;

import net.errantchaosie.tradeandopenmarkets.shop.tab.config.ShopTabDefinition;

/**
 * Factory interface for producing concrete shop tabs from configuration data.
 */
public interface ShopTabType {
    /**
     * @return the id that uniquely identifies this tab type.
     */
    net.minecraft.resources.ResourceLocation id();

    /**
     * Creates a tab instance from the provided configuration.
     */
    ShopTab create(ShopTabDefinition definition, ShopTabGenerationContext context);
}
