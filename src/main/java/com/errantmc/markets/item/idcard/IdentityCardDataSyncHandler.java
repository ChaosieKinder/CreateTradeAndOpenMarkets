package com.errantmc.markets.item.idcard;

import com.errantmc.markets.Config;
import com.errantmc.markets.compat.CuriosCompat;
import com.errantmc.markets.component.IdentityCardComponent;
import com.errantmc.markets.item.wallet.WalletInventory;
import com.errantmc.markets.item.wallet.WalletItem;
import com.errantmc.markets.network.TradeNetwork;
import com.errantmc.markets.network.message.IdentityCardPlayerInfoSyncMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Sends each player the live data shown on the identity cards they carry, and nothing else
 * (BACKLOG #17). A snapshot includes its subject's position and respawn point, so it goes only to
 * someone holding that person's card; never broadcast.
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
            for (ServerPlayer player : server.getPlayerList().getPlayers()) sendTo(player);
        }
        ticksByServer.put(server, ticks);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sendTo(player);
    }

    /**
     * Sends {@code player} the snapshots for the cards they carry, replacing what their client had;
     * an empty list clears it. Also called when a card is bound.
     */
    public static void sendTo(ServerPlayer player) {
        TradeNetwork.sendTo(player, new IdentityCardPlayerInfoSyncMessage(snapshotsFor(player)));
    }

    /** Snapshots of the online owners of every card {@code viewer} carries. */
    public static List<IdentityCardPlayerSnapshot> snapshotsFor(ServerPlayer viewer) {
        List<IdentityCardPlayerSnapshot> out = new ArrayList<>();
        for (UUID owner : cardOwnersCarriedBy(viewer)) {
            ServerPlayer subject = viewer.server.getPlayerList().getPlayer(owner);
            if (subject != null) out.add(IdentityCardPlayerSnapshot.from(subject));
        }
        return out;
    }

    /** Whose cards {@code player} carries: inventory, armour and offhand, inside wallets, and worn curios. */
    public static Set<UUID> cardOwnersCarriedBy(ServerPlayer player) {
        Set<UUID> owners = new LinkedHashSet<>();
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) collect(inv.getItem(i), owners);
        if (CuriosCompat.isLoaded()) {
            for (ItemStack worn : CuriosCompat.allCurios(player)) collect(worn, owners);
        }
        return owners;
    }

    private static void collect(ItemStack stack, Set<UUID> owners) {
        IdentityCardComponent.get(stack).ifPresent(card -> owners.add(card.playerId()));
        if (stack.getItem() instanceof WalletItem) {
            for (ItemStack inside : WalletInventory.read(stack)) {
                IdentityCardComponent.get(inside).ifPresent(card -> owners.add(card.playerId()));
            }
        }
    }
}
