package com.errantmc.markets.item;

import com.errantmc.markets.block.TradeBlocks;
import com.errantmc.markets.block.entity.CoinBlockEntity;
import com.errantmc.markets.component.CurrencyData;
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

    public static long totalCoins(ItemStack stack) { return CurrencyData.getFrom(stack).totalCount(); }
    public static double totalValue(ItemStack stack) { return CurrencyData.getFrom(stack).totalValue(); }

    /** Map to 1..4 for model predicate */
    public static int tier(ItemStack stack) {
        long coins = totalCoins(stack);
        double value = totalValue(stack);
        // Choose whichever heuristic you prefer. Example mixes both:
        if (value > 100000000) return 4;
        if (value > 10000000) return 3;
        if (value > 100000) return 2;
        return 1;
    }

    public CoinStackItem(Properties props) { super(props); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        var c = CurrencyData.getFrom(stack);
        tip.add(Component.literal("Coins: " + c.totalCount()).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Value: " + c.totalValue()).withStyle(ChatFormatting.GOLD));
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
            be.setCurrency(CurrencyData.getFrom(ctx.getItemInHand()));
        }

        Player player = ctx.getPlayer();
        if (player == null || !player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
