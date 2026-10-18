package com.errantmc.markets.item.idcard.client;

import com.errantmc.markets.item.idcard.IdentityCardPlayerSnapshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client side cache of the latest dynamic identity card data for players.
 */
@OnlyIn(Dist.CLIENT)
public final class IdentityCardClientCache {
    private static final Map<UUID, IdentityCardPlayerSnapshot> CACHE = new ConcurrentHashMap<>();

    private IdentityCardClientCache() {
    }

    public static Optional<IdentityCardPlayerSnapshot> get(UUID playerId) {
        return Optional.ofNullable(CACHE.get(playerId));
    }

    /** Each sync is the whole set the server allows this client to see, so it replaces the cache. */
    public static void replaceAll(Collection<IdentityCardPlayerSnapshot> snapshots) {
        CACHE.clear();
        for (IdentityCardPlayerSnapshot snapshot : snapshots) {
            CACHE.put(snapshot.playerId(), snapshot);
        }
    }

    /** On leaving a server: nothing from it may show on the next. */
    public static void clear() {
        CACHE.clear();
    }
}