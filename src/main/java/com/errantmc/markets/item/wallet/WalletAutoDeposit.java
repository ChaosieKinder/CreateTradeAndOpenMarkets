package com.errantmc.markets.item.wallet;

import com.errantmc.markets.compat.CuriosCompat;
import com.errantmc.markets.component.Purses;
import com.errantmc.markets.item.CoinItem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

import javax.annotation.Nullable;
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

        ItemStack wallet = findWallet(player);
        if (wallet == null) return;

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

    private static @Nullable ItemStack findWallet(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof WalletItem) return main;
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof WalletItem) return off;

        for (ItemStack s : player.getInventory().items) {
            if (s.getItem() instanceof WalletItem) return s;
        }

        // Mutating a curio stack's components in place is enough; Curios syncs it
        if (CuriosCompat.isLoaded()) {
            return CuriosCompat.findCurio(player, s -> s.getItem() instanceof WalletItem);
        }
        return null;
    }
}
