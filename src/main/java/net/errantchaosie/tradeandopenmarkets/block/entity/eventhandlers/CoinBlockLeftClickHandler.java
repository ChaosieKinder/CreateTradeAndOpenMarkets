package net.errantchaosie.tradeandopenmarkets.block.entity.eventhandlers;

import net.errantchaosie.tradeandopenmarkets.block.TAOMBlocks;
import net.errantchaosie.tradeandopenmarkets.block.entity.CoinBlockEntity;
import net.errantchaosie.tradeandopenmarkets.component.CurrencyData;
import net.errantchaosie.tradeandopenmarkets.item.CoinItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CoinBlockLeftClickHandler {

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock e) {
        Level level = e.getLevel();
        BlockPos pos = e.getPos();
        BlockState state = level.getBlockState(pos);

        if (!state.is(TAOMBlocks.COIN_BLOCK.get())) return;

        // Always cancel on client so the hand animation plays but mining doesn't start
        if (level.isClientSide) {
            e.setCanceled(true);
            //e.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CoinBlockEntity coinBE)) return;

        // If the block has no coins, let normal breaking proceed
        if (coinBE.getCurrency().isEmpty()) return;

        // Take ONE coin of the highest denomination and give to player
        Player player = e.getEntity();

        boolean took = player.isCrouching()
                ? dropFromHighestCoin(coinBE, level, pos, 64)
                : dropFromHighestCoin(coinBE, level, pos, 1);

        if (took) {
            if(coinBE.getCurrency().isEmpty()) {
                level.destroyBlock(pos, false, e.getEntity());
                return;
            }

            coinBE.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
            // prevent breaking/progress
            e.setCanceled(true);
        }
    }

    /** Removes one coin from the highest non-empty stack and gives it to the player. */
    private static boolean dropFromHighestCoin(CoinBlockEntity be, Level level, BlockPos pos, int count) {
        CurrencyData c = be.getCurrency();
        var denom = c.getHighestDenomonation();
        var result = c.remove(denom, count);
        if(result.delta() > 0) {
            be.setCurrency(result.updated());
            spawnDrop(level, pos, CoinItem.createCurrencyItem(denom, result.delta()));
            return true;
        }
        return false;
    }

    /** Spawns an ItemEntity at the block center with a short pickup delay and a little upward nudge. */
    private static void spawnDrop(Level level, BlockPos pos, ItemStack stack) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.7; // slight above floor
        double z = pos.getZ() + 0.5;

        ItemEntity entity = new ItemEntity(level, x, y, z, stack);
        entity.setPickUpDelay(4);        // half-second pickup delay
        entity.setDeltaMovement(0, 0.05, 0); // tiny hop

        level.addFreshEntity(entity);
        level.playSound(null, x, y, z, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.25f, 0.9f);
    }
}
