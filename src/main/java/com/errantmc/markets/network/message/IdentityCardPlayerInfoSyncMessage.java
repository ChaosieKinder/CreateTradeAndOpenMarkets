package com.errantmc.markets.network.message;

import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.item.idcard.IdentityCardPlayerSnapshot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Synchronises dynamic player data to clients for identity cards.
 */
public class IdentityCardPlayerInfoSyncMessage implements CustomPacketPayload {
    public static final Type<IdentityCardPlayerInfoSyncMessage> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ErrantTrade.MOD_ID, "identity_card_player_info_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, IdentityCardPlayerInfoSyncMessage> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public IdentityCardPlayerInfoSyncMessage decode(RegistryFriendlyByteBuf buf) {
            return IdentityCardPlayerInfoSyncMessage.decode(buf);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, IdentityCardPlayerInfoSyncMessage message) {
            IdentityCardPlayerInfoSyncMessage.encode(message, buf);
        }
    };

    private final List<IdentityCardPlayerSnapshot> snapshots;

    public IdentityCardPlayerInfoSyncMessage(Collection<IdentityCardPlayerSnapshot> snapshots) {
        this.snapshots = List.copyOf(snapshots);
    }

    public List<IdentityCardPlayerSnapshot> snapshots() {
        return snapshots;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(IdentityCardPlayerInfoSyncMessage message, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(message.snapshots.size());
        for (IdentityCardPlayerSnapshot snapshot : message.snapshots) {
            snapshot.encode(buf);
        }
    }

    private static IdentityCardPlayerInfoSyncMessage decode(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<IdentityCardPlayerSnapshot> snapshots = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            snapshots.add(IdentityCardPlayerSnapshot.decode(buf));
        }
        return new IdentityCardPlayerInfoSyncMessage(snapshots);
    }

    public static void handle(IdentityCardPlayerInfoSyncMessage message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                com.errantmc.markets.item.idcard.client.IdentityCardClientCache.updateAll(message.snapshots);
            }
        });
    }
}