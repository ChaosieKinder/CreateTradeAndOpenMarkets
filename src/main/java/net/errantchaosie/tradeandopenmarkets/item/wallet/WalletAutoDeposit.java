package net.errantchaosie.tradeandopenmarkets.item.wallet;

import com.mojang.logging.LogUtils;
import net.errantchaosie.tradeandopenmarkets.component.WalletBalance;
import net.errantchaosie.tradeandopenmarkets.data.CoinDenomination;
import net.errantchaosie.tradeandopenmarkets.item.CoinItem;
import net.minecraft.core.NonNullList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jline.utils.Log;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.List;

public final class WalletAutoDeposit {
    public static final Logger LOG = LogUtils.getLogger();
    private WalletAutoDeposit(){
    }

    public static void onPlayerTick(PlayerTickEvent.Post e) {

        Player player = e.getEntity();
        Level level = player.level();
        if (level.isClientSide) return;

        // Find a wallet (hands → hotbar; add Curios scan if you use Curios)
        ItemStack wallet = findWallet(player);
        if (wallet == null) return;

        // Find coin items overlapping the player's pickup box (same as vanilla does)
        // A small inflate helps catch near-misses; tweak if needed.
        var box = player.getBoundingBox().inflate(0.2);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box, ie ->
                ie.isAlive() && !ie.hasPickUpDelay() && ie.getItem().getItem() instanceof CoinItem
        );
        if (items.isEmpty()) return;

        boolean any = false;
        for (ItemEntity ie : items) {
            ItemStack ground = ie.getItem();

            if (ground.isEmpty()) continue;
            CoinDenomination d = ((CoinItem) ground.getItem()).denom();
            int count = ground.getCount();
            if (count <= 0) continue;

            // Deposit entire stack; adjust if you enforce a capacity
            WalletBalance.ensure(wallet);
            WalletBalance.add(wallet, d, count);

            // Remove the ground entity (consumed by the wallet)
            ie.discard();
            any = true;
        }

        if (any) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1.0f);
        }
    }

    private static ItemStack findWallet(Player player) {
        // Hands first
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof WalletItem) return main;
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof WalletItem) return off;

        // Hotbar
        NonNullList<ItemStack> items = player.getInventory().items;
        for (ItemStack s : items) {
            if (s.getItem() instanceof WalletItem) return s;
        }

        // Curios (soft)
        if (ModList.get().isLoaded("curios")) {
            var opt = CuriosApi.getCuriosInventory(player);
            if (opt.isPresent()) {
                ICuriosItemHandler inv = opt.orElseThrow();
                for (var entry : inv.getCurios().entrySet()) {
                    IDynamicStackHandler stacks = entry.getValue().getStacks();
                    for (int i = 0; i < stacks.getSlots(); i++) {
                        ItemStack s = stacks.getStackInSlot(i);
                        if (!s.isEmpty() && s.getItem() instanceof WalletItem) {
                            return s; // Works: we only mutate its Data Components
                        }
                    }
                }
            }
        }

        return null;
    }
}
