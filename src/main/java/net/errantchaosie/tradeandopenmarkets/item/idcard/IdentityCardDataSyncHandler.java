package net.errantchaosie.tradeandopenmarkets.item.idcard;

import net.errantchaosie.tradeandopenmarkets.Config;
import net.errantchaosie.tradeandopenmarkets.network.TAOMNetwork;
import net.errantchaosie.tradeandopenmarkets.network.message.IdentityCardPlayerInfoSyncMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles synchronising player data for identity cards from the server to clients.
 */
public class IdentityCardDataSyncHandler {
    private final Map<MinecraftServer, Integer> ticksByServer = new IdentityHashMap<>();

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int interval = Math.max(1, Config.IDENTITY_CARD_SYNC_INTERVAL_TICKS.get());
        int ticks = ticksByServer.getOrDefault(server, 0) + 1;
        if (ticks >= interval) {
            ticks = 0;
            broadcastToAll(server);
        }
        ticksByServer.put(server, ticks);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.server;
        if (server == null) {
            return;
        }
        sendToPlayer(player);
    }

    private static void broadcastToAll(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return;
        }

        IdentityCardPlayerInfoSyncMessage message = new IdentityCardPlayerInfoSyncMessage(
                players.stream().map(IdentityCardPlayerSnapshot::from).toList()
        );
        TAOMNetwork.sendToAll(message);
    }

    private static void sendToPlayer(ServerPlayer player) {
        MinecraftServer server = player.server;
        if (server == null) {
            return;
        }

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return;
        }

        IdentityCardPlayerInfoSyncMessage message = new IdentityCardPlayerInfoSyncMessage(
                players.stream().map(IdentityCardPlayerSnapshot::from).toList()
        );
        TAOMNetwork.sendTo(player, message);
    }
}