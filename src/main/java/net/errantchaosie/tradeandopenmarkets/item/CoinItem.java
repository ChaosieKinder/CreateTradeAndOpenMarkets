package net.errantchaosie.tradeandopenmarkets.item;

import com.mojang.logging.LogUtils;
import net.errantchaosie.tradeandopenmarkets.block.TAOMBlocks;
import net.errantchaosie.tradeandopenmarkets.block.entity.CoinBlockEntity;
import net.errantchaosie.tradeandopenmarkets.component.CurrencyData;
import net.errantchaosie.tradeandopenmarkets.data.CoinDenomination;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.slf4j.Logger;

import java.util.List;

public class CoinItem extends Item {
    public static final Logger LOG = LogUtils.getLogger();


    private final CoinDenomination denom;

    public CoinDenomination denom() { return denom; }

    public CoinItem(Properties properties, CoinDenomination denom) {
        super(properties);
        this.denom = denom;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx,
                                List<Component> tooltip, TooltipFlag flag) {
        int total = stack.getCount() * denom.value;
        tooltip.add(Component.translatable("tooltip_info_item.coin")
                .append(Component.literal(denom.format))
                .append(Component.literal(String.valueOf(total))));
        super.appendHoverText(stack, ctx, tooltip, flag);
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        // Allow block activation while sneaking when clicking a CoinBlock
        return level.getBlockState(pos).is(TAOMBlocks.COIN_BLOCK.get());
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        Level level   = ctx.getLevel();
        BlockPos pos  = ctx.getClickedPos();

        // If clicking an existing CoinBlock, let the block handle (single or double tap)
        //  and don't allow placing coin blocks on top of coin blocks
        if (level.getBlockState(pos).is(TAOMBlocks.COIN_BLOCK.get())) {
            return InteractionResult.PASS;
        }

        // Build placement context so the block can choose its proper state (orientation, etc.)
        BlockPlaceContext placeCtx = new BlockPlaceContext(ctx);
        BlockState toPlace = TAOMBlocks.COIN_BLOCK.get().getStateForPlacement(placeCtx);

        if (toPlace == null) {
            // Fallback if your block doesn’t use orientation
            toPlace = TAOMBlocks.COIN_BLOCK.get().defaultBlockState();
        }

        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockPos placePos = pos.above();
        // Place the block (flag 11: neighbors + client update)
        boolean placed = level.setBlock(placePos, toPlace, 11);
        if (!placed) return InteractionResult.FAIL;

        level.gameEvent(player, GameEvent.BLOCK_PLACE, placePos);

        // Initialize the just-created BlockEntity with 1 coin of this denomination
        if (level.getBlockEntity(placePos) instanceof CoinBlockEntity be) {
            be.setCurrency(
                    CurrencyData.newCurrencyFromDenom(
                            getDenomination(ctx.getItemInHand().getItem()), 1));
            be.setChanged();
            level.sendBlockUpdated(placePos, toPlace, toPlace, 3);
        }

        // Consume one coin unless in creative
        ItemStack held = ctx.getItemInHand();
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    public static CoinDenomination getDenomination(Item item) {
        if (item == TAOMItems.COIN_COPPER.get())   return CoinDenomination.COPPER;
        if (item == TAOMItems.COIN_BRONZE.get())   return CoinDenomination.BRONZE;
        if (item == TAOMItems.COIN_SILVER.get())   return CoinDenomination.SILVER;
        if (item == TAOMItems.COIN_GOLD.get()) return CoinDenomination.GOLD;
        if (item == TAOMItems.COIN_ELECTRUM.get()) return CoinDenomination.ELECTRUM;
        if (item == TAOMItems.COIN_PLATINUM.get()) return CoinDenomination.PLATINUM;
        if (item == TAOMItems.COIN_EMERALD.get())  return CoinDenomination.EMERALD;
        return CoinDenomination.NONE;
    }

    public static ItemStack createCurrencyItem(CoinDenomination denomination, int value ) {
        if (denomination == CoinDenomination.COPPER) return new ItemStack(TAOMItems.COIN_COPPER.get(), value);
        if (denomination == CoinDenomination.BRONZE) return new ItemStack(TAOMItems.COIN_BRONZE.get(), value);
        if (denomination == CoinDenomination.SILVER) return new ItemStack(TAOMItems.COIN_SILVER.get(), value);
        if (denomination == CoinDenomination.GOLD) return new ItemStack(TAOMItems.COIN_GOLD.get(), value);
        if (denomination == CoinDenomination.ELECTRUM) return new ItemStack(TAOMItems.COIN_ELECTRUM.get(), value);
        if (denomination == CoinDenomination.PLATINUM) return new ItemStack(TAOMItems.COIN_PLATINUM.get(), value);
        if (denomination == CoinDenomination.EMERALD) return new ItemStack(TAOMItems.COIN_EMERALD.get(), value);
        return ItemStack.EMPTY;
    }

    public static boolean isCoinItem(Item item) {
        return item == TAOMItems.COIN_COPPER.get()
                || item == TAOMItems.COIN_BRONZE.get()
                || item == TAOMItems.COIN_SILVER.get()
                || item == TAOMItems.COIN_GOLD.get()
                || item == TAOMItems.COIN_ELECTRUM.get()
                || item == TAOMItems.COIN_PLATINUM.get()
                || item == TAOMItems.COIN_EMERALD.get();
    }
}
