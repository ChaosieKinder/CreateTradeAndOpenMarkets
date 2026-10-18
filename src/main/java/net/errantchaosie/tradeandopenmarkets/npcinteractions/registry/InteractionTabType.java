package net.errantchaosie.tradeandopenmarkets.npcinteractions.registry;

import net.errantchaosie.tradeandopenmarkets.npcinteractions.components.InteractionTab;
import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * Factory interface for producing concrete npcinteractions tabs from configuration data.
 */
public interface InteractionTabType {
    /**
     * @return the id that uniquely identifies this tabs type.
     */
    net.minecraft.resources.ResourceLocation id();

    /**
     * Creates a tabs instance from the provided configuration.
     */
    InteractionTab create(InteractionTabConfig definition, InteractionTabGenerationContext context);

    /**
     * Creates a tabs instance by reading from a network buffer.
     */
    InteractionTab readFromBuffer(RegistryFriendlyByteBuf buffer);
}
