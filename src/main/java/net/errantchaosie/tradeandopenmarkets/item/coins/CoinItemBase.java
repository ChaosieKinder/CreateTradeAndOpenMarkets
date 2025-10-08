package net.errantchaosie.tradeandopenmarkets.item.coins;

import com.mojang.logging.LogUtils;
import net.errantchaosie.tradeandopenmarkets.block.TAOMBlocks;
import net.errantchaosie.tradeandopenmarkets.block.entity.CoinBlockEntity;
import net.errantchaosie.tradeandopenmarkets.component.CurrencyInfo;
import net.errantchaosie.tradeandopenmarkets.item.TAOMItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;


public class CoinItemBase extends Item {
    public static final Logger LOG = LogUtils.getLogger();
    public enum Denomination { NONE, COPPER, BRONZE, SILVER, ELECTRUM, EMERALD }

    public CoinItemBase(Properties properties) {
        super(properties);
    }


    public static Denomination getDenomination(Item item) {
        if (item == TAOMItems.COIN_COPPER.get())   return Denomination.COPPER;
        if (item == TAOMItems.COIN_BRONZE.get())   return Denomination.BRONZE;
        if (item == TAOMItems.COIN_SILVER.get())   return Denomination.SILVER;
        if (item == TAOMItems.COIN_ELECTRUM.get()) return Denomination.ELECTRUM;
        if (item == TAOMItems.COIN_EMERALD.get())  return Denomination.EMERALD;
        return Denomination.NONE;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        Level level   = ctx.getLevel();

        // must have a player, must be crouching (shift), must click the TOP face
        if (player == null || !player.isCrouching() || ctx.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }

        BlockPos basePos  = ctx.getClickedPos();
        BlockPos placePos = basePos.above();

        // Build placement context so the block can choose its proper state (orientation, etc.)
        BlockPlaceContext placeCtx = new BlockPlaceContext(ctx);
        BlockState toPlace = TAOMBlocks.COIN_BLOCK.get().getStateForPlacement(placeCtx);
        if (toPlace == null) {
            // Fallback if your block doesn’t use orientation
            toPlace = TAOMBlocks.COIN_BLOCK.get().defaultBlockState();
        }

        // Check replaceability at the target position
        if (!level.getBlockState(placePos).canBeReplaced(placeCtx)) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide) {
            // Place the block (flag 11: neighbors + client update)
            boolean placed = level.setBlock(placePos, toPlace, 11);
            if (!placed) return InteractionResult.FAIL;

            level.gameEvent(player, GameEvent.BLOCK_PLACE, placePos);

            // Initialize the just-created BlockEntity with 1 coin of this denomination
            if (level.getBlockEntity(placePos) instanceof CoinBlockEntity be) {
                be.setCurrency(newCurrencyFromDenom(getDenomination(ctx.getItemInHand().getItem()), 1, Optional.of(player.getUUID())));
                be.setChanged();
                level.sendBlockUpdated(placePos, toPlace, toPlace, 3);
            }

            // Consume one coin unless in creative
            ItemStack held = ctx.getItemInHand();
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Build a CurrencyInfo with exactly `amount` in the given denomination. */
    private static CurrencyInfo newCurrencyFromDenom(Denomination d, int amount, Optional<UUID> owner) {
        int c = 0, b = 0, s = 0, e = 0, em = 0;
        switch (d) {
            case COPPER   -> c  = amount;
            case BRONZE   -> b  = amount;
            case SILVER   -> s  = amount;
            case ELECTRUM -> e  = amount;
            case EMERALD  -> em = amount;
        }
        return new CurrencyInfo(c, b, s, e, em, owner);
    }
}
