package net.errantchaosie.tradeandopenmarkets.item.idcard.client;

import net.errantchaosie.tradeandopenmarkets.item.idcard.IdentityCardPlayerSnapshot;
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

    public static void updateAll(Collection<IdentityCardPlayerSnapshot> snapshots) {
        for (IdentityCardPlayerSnapshot snapshot : snapshots) {
            CACHE.put(snapshot.playerId(), snapshot);
        }
    }
}