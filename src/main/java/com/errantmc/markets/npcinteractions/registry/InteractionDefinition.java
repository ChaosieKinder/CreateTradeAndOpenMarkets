package com.errantmc.markets.npcinteractions.registry;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Represents the configured tabs for a particular merchant profession.
 */
public record InteractionDefinition(ResourceLocation profession, List<InteractionTabConfig> tabs) {
    public InteractionDefinition {
        tabs = List.copyOf(tabs);
    }
}
