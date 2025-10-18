package net.errantchaosie.tradeandopenmarkets.network;

import net.errantchaosie.tradeandopenmarkets.network.message.IdentityCardPlayerInfoSyncMessage;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Centralised registration and helpers for the mod network channel.
 */
public final class TAOMNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private TAOMNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(
                IdentityCardPlayerInfoSyncMessage.TYPE,
                IdentityCardPlayerInfoSyncMessage.STREAM_CODEC,
                IdentityCardPlayerInfoSyncMessage::handle
        );
    }

    public static void sendToAll(IdentityCardPlayerInfoSyncMessage message) {
        PacketDistributor.sendToAllPlayers(message);
    }

    public static void sendTo(ServerPlayer player, IdentityCardPlayerInfoSyncMessage message) {
        PacketDistributor.sendToPlayer(player, message);
    }
}