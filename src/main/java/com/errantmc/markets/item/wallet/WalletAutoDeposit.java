package com.errantmc.markets.item.wallet;

import com.errantmc.markets.component.Purses;
import com.errantmc.markets.item.CoinItem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

import java.util.UUID;

/** Coins a player picks up go into the first wallet they carry instead of the inventory. */
public final class WalletAutoDeposit {

    /**
     * Fires whenever a player touches an item entity, but before vanilla checks the pickup delay
     * and the item's target, so both are checked here.
     */
    @SubscribeEvent
    public void onItemPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        ItemEntity entity = event.getItemEntity();
        if (player.level().isClientSide) return;

        ItemStack ground = entity.getItem();
        if (!(ground.getItem() instanceof CoinItem coin)) return;
        UUID target = entity.getTarget();
        if (entity.hasPickUpDelay() || (target != null && !target.equals(player.getUUID()))) return;

        ItemStack wallet = WalletFinder.findWallet(player);
        if (wallet.isEmpty()) return;

        var result = Purses.add(wallet, coin.denom(), ground.getCount());
        if (result.moved() == 0) return; // wallet full: vanilla picks the coins up as usual

        player.take(entity, result.moved()); // pickup animation and sound for everyone watching
        ground.shrink(result.moved());
        if (ground.isEmpty()) {
            entity.discard();
            event.setCanPickup(TriState.FALSE);
        }
        // otherwise vanilla picks up what did not fit
    }
}
