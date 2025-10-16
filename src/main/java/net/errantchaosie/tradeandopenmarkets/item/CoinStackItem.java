package net.errantchaosie.tradeandopenmarkets.item;

import net.errantchaosie.tradeandopenmarkets.block.TAOMBlocks;
import net.errantchaosie.tradeandopenmarkets.block.entity.CoinBlockEntity;
import net.errantchaosie.tradeandopenmarkets.component.CurrencyData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockPos place = ctx.getClickedPos().relative(ctx.getClickedFace());
        if (!level.getBlockState(place).canBeReplaced()) return InteractionResult.FAIL;

        BlockState state = TAOMBlocks.COIN_BLOCK.get().defaultBlockState();
        level.setBlock(place, state, Block.UPDATE_ALL_IMMEDIATE);

        if (level.getBlockEntity(place) instanceof CoinBlockEntity be) {
            be.setCurrency(CurrencyData.getFrom(ctx.getItemInHand()));
            be.setChanged();
        }

        if (!ctx.getPlayer().getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
