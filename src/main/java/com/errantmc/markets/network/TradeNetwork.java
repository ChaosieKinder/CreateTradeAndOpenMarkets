package com.errantmc.markets.network;

import com.errantmc.markets.network.message.IdentityCardPlayerInfoSyncMessage;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Centralised registration and helpers for the mod network channel.
 */
public final class TradeNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private TradeNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(
                IdentityCardPlayerInfoSyncMessage.TYPE,
                IdentityCardPlayerInfoSyncMessage.STREAM_CODEC,
                IdentityCardPlayerInfoSyncMessage::handle
        );
    }

    public static void sendToAll(MinecraftServer server, CustomPacketPayload message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendTo(player, message);
        }
    }

    /**
     * Skips players whose connection never negotiated our channel (fake players, gametest mock
     * players); sending to one of them throws.
     */
    public static void sendTo(ServerPlayer player, CustomPacketPayload message) {
        if (player.connection != null && player.connection.hasChannel(message)) {
            PacketDistributor.sendToPlayer(player, message);
        }
    }
}