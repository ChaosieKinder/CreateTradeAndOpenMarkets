package net.errantchaosie.tradeandopenmarkets.npcinteractions.registry;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Simple runtime registry used to keep track of all available {@link InteractionTabType} instances.
 */
public final class InteractionTabRegistry {
    private static final Map<ResourceLocation, InteractionTabType> TYPES = new LinkedHashMap<>();

    private InteractionTabRegistry() {
    }

    public static InteractionTabType register(InteractionTabType type) {
        ResourceLocation id = type.id();
        InteractionTabType previous = TYPES.putIfAbsent(id, type);
        if (previous != null) {
            throw new IllegalStateException("Duplicate npcinteractions tabs type registration for " + id);
        }
        return type;
    }

    @Nullable
    public static InteractionTabType get(ResourceLocation id) {
        return TYPES.get(id);
    }

    public static Optional<InteractionTabType> maybeGet(ResourceLocation id) {
        return Optional.ofNullable(TYPES.get(id));
    }

    public static Collection<InteractionTabType> values() {
        return Collections.unmodifiableCollection(TYPES.values());
    }
}
