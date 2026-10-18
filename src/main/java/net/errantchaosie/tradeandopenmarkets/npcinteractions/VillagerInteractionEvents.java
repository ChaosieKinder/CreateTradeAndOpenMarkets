package net.errantchaosie.tradeandopenmarkets.npcinteractions;

import net.errantchaosie.tradeandopenmarkets.npcinteractions.registry.InteractionDefinitionLoader;
import net.errantchaosie.tradeandopenmarkets.npcinteractions.registry.InteractionTabManager;
import net.errantchaosie.tradeandopenmarkets.screen.villager.VillagerInteractionMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class VillagerInteractionEvents {

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Villager villager)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Stop vanilla from opening its MerchantMenu
        event.setCanceled(true);

        // Open the custom menu instead
        openVillagerInteraction(player, villager);
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(InteractionDefinitionLoader.getInstance());
    }

    private static void openVillagerInteraction(ServerPlayer player, Villager villager) {
        player.openMenu(// MenuProvider (server-side menu + title)
            new SimpleMenuProvider(
                (containerId, playerInv, p) -> new VillagerInteractionMenu(containerId, playerInv, villager),
                villager.getDisplayName()
            ),
            // Extra data sent to CLIENT menu ctor
            buf -> {
                // NOTE: on 1.21+ this is RegistryFriendlyByteBuf
                buf.writeVarInt(villager.getId());

                VillagerProfession prof = villager.getVillagerData().getProfession();
                ResourceLocation key = BuiltInRegistries.VILLAGER_PROFESSION.getKey(prof);
                buf.writeResourceLocation(key);

                var tabs = InteractionTabManager.INSTANCE.resolveTabs(villager, villager.getOffers(), player.level());
                InteractionTabManager.INSTANCE.writeToStream(buf, tabs);
            });
    }
}
