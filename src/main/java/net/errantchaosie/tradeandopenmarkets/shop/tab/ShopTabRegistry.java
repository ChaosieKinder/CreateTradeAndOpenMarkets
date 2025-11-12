package net.errantchaosie.tradeandopenmarkets.shop.tab;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Simple runtime registry used to keep track of all available {@link ShopTabType} instances.
 */
public final class ShopTabRegistry {
    private static final Map<ResourceLocation, ShopTabType> TYPES = new LinkedHashMap<>();

    private ShopTabRegistry() {
    }

    public static ShopTabType register(ShopTabType type) {
        ResourceLocation id = type.id();
        ShopTabType previous = TYPES.putIfAbsent(id, type);
        if (previous != null) {
            throw new IllegalStateException("Duplicate shop tab type registration for " + id);
        }
        return type;
    }

    @Nullable
    public static ShopTabType get(ResourceLocation id) {
        return TYPES.get(id);
    }

    public static Optional<ShopTabType> maybeGet(ResourceLocation id) {
        return Optional.ofNullable(TYPES.get(id));
    }

    public static Collection<ShopTabType> values() {
        return Collections.unmodifiableCollection(TYPES.values());
    }
}
