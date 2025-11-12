package net.errantchaosie.tradeandopenmarkets.shop.tab.config;

import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Parsed configuration describing a tab entry within a shop definition.
 */
public record ShopTabDefinition(ResourceLocation type, Component title, JsonObject data) {
}
