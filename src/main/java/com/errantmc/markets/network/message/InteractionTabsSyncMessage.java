package com.errantmc.markets.network.message;

import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.npcinteractions.components.InteractionTab;
import com.errantmc.markets.npcinteractions.registry.InteractionTabManager;
import com.errantmc.markets.screen.villager.VillagerInteractionMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** The server's tabs after a trade changed them, for the client's open interaction menu. */
public record InteractionTabsSyncMessage(int containerId, List<InteractionTab> tabs) implements CustomPacketPayload {
    public static final Type<InteractionTabsSyncMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ErrantTrade.MOD_ID, "interaction_tabs_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InteractionTabsSyncMessage> STREAM_CODEC = StreamCodec.of(
            (buf, message) -> {
                buf.writeVarInt(message.containerId());
                InteractionTabManager.INSTANCE.writeToStream(buf, message.tabs());
            },
            buf -> new InteractionTabsSyncMessage(buf.readVarInt(), InteractionTabManager.INSTANCE.readFromStream(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(InteractionTabsSyncMessage message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof VillagerInteractionMenu menu
                    && menu.containerId == message.containerId()) {
                menu.setTabs(message.tabs());
            }
        });
    }
}
