package net.errantchaosie.tradeandopenmarkets.shop.tab;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Represents a concrete tab shown in the shop screen.
 */
public interface ShopTab {
    /**
     * @return the identifier of the tab type that produced this tab instance.
     */
    ResourceLocation type();

    /**
     * @return the display name shown on the tab button.
     */
    Component title();
}
