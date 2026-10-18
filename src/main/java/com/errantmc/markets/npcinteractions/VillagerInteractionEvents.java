package com.errantmc.markets.npcinteractions;

import com.errantmc.markets.npcinteractions.registry.InteractionDefinitionLoader;
import com.errantmc.markets.npcinteractions.registry.InteractionTabManager;
import com.errantmc.markets.screen.villager.VillagerInteractionMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Opens the mod's interaction menu where vanilla would have opened its trading screen. */
public final class VillagerInteractionEvents {

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Villager villager)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!opensTrading(player, villager, event.getItemStack())) return;

        event.setCanceled(true); // vanilla's MerchantMenu must not open as well
        open(player, villager);
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(InteractionDefinitionLoader.getInstance());
    }

    /**
     * Take over only the trade itself, where vanilla's {@code Villager.mobInteract} would open its
     * trading screen. Everything else stays vanilla's: name tags and leads (handled before trading),
     * spawn eggs, sneaking, babies, sleeping villagers, one already trading with someone else, and
     * the unemployed and nitwits, who have nothing to sell.
     */
    static boolean opensTrading(ServerPlayer player, Villager villager, ItemStack held) {
        if (!villager.isAlive() || villager.isBaby() || villager.isSleeping() || villager.isTrading()) return false;
        if (player.isSecondaryUseActive()) return false;
        if (held.is(Items.NAME_TAG) || held.is(Items.LEAD) || held.getItem() instanceof SpawnEggItem) return false;
        VillagerProfession profession = villager.getVillagerData().getProfession();
        return profession != VillagerProfession.NONE && profession != VillagerProfession.NITWIT;
    }

    private static void open(ServerPlayer player, Villager villager) {
        var tabs = InteractionTabManager.INSTANCE.resolveTabs(villager, villager.getOffers(), player.level());
        // Claimed as vanilla does, so the villager stands still and no one else can trade with it
        villager.setTradingPlayer(player);
        player.openMenu(
                new SimpleMenuProvider(
                        (containerId, playerInv, p) -> new VillagerInteractionMenu(containerId, playerInv, villager, tabs),
                        villager.getDisplayName()),
                // Read by the client's menu constructor: the server's tabs are the only tabs
                buf -> {
                    buf.writeVarInt(villager.getId());
                    buf.writeResourceLocation(BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession()));
                    InteractionTabManager.INSTANCE.writeToStream(buf, tabs);
                });
    }
}
