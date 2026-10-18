package com.errantmc.markets.item;

import com.errantmc.markets.block.TradeBlocks;
import com.errantmc.markets.block.entity.CoinBlockEntity;
import com.errantmc.markets.component.Purses;
import com.errantmc.markets.domain.MoneyFormat;
import com.errantmc.markets.domain.Purse;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class CoinStackItem extends Item {

    /** Which of the four models a coin stack shows, by worth: over $100,000, $10M and $100M. */
    public static int tier(ItemStack stack) {
        long copperBits = Purses.get(stack).totalValue();
        if (copperBits > 10_000_000_000L) return 4;
        if (copperBits > 1_000_000_000L) return 3;
        if (copperBits > 10_000_000L) return 2;
        return 1;
    }

    public CoinStackItem(Properties props) { super(props); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        Purse purse = Purses.get(stack);
        tip.add(Component.translatable("tooltip.erranttrade.coin_stack.coins", purse.totalCount()).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("tooltip.erranttrade.coin_stack.value", MoneyFormat.format(purse.totalValue()))
                .withStyle(ChatFormatting.GOLD));
    }

    /** Place CoinBlock and move component data to the BE */
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();

        // Only where the pile can survive: an unsupported one drops its whole balance on the next update
        BlockPlaceContext placeCtx = new BlockPlaceContext(ctx);
        BlockPos place = placeCtx.getClickedPos();
        BlockState state = TradeBlocks.COIN_PILE.get().getStateForPlacement(placeCtx);
        if (!placeCtx.canPlace() || state == null || !state.canSurvive(level, place)) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (!level.setBlock(place, state, Block.UPDATE_ALL_IMMEDIATE)) return InteractionResult.FAIL;
        if (level.getBlockEntity(place) instanceof CoinBlockEntity be) {
            be.setCurrency(Purses.get(ctx.getItemInHand()));
        }

        Player player = ctx.getPlayer();
        if (player == null || !player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
