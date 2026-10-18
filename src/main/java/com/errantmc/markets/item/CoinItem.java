package com.errantmc.markets.item;

import com.mojang.logging.LogUtils;
import com.errantmc.markets.block.TradeBlocks;
import com.errantmc.markets.block.entity.CoinBlockEntity;
import com.errantmc.markets.domain.CoinDenomination;
import com.errantmc.markets.domain.MoneyFormat;
import com.errantmc.markets.domain.Purse;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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
        long total = (long) stack.getCount() * denom.value;
        tooltip.add(Component.translatable("item.erranttrade." + denom.id + ".tooltip")
                .append("\n")
                .append(Component.translatable("tooltip_info_item.coin"))
                .append(Component.literal(MoneyFormat.format(total)).withStyle(CoinStyle.colour(denom))));
        super.appendHoverText(stack, ctx, tooltip, flag);
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        // Allow block activation while sneaking when clicking a CoinBlock
        return level.getBlockState(pos).is(TradeBlocks.COIN_PILE.get());
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        Level level   = ctx.getLevel();

        // Clicking an existing pile is the pile's business (single and double tap)
        if (level.getBlockState(ctx.getClickedPos()).is(TradeBlocks.COIN_PILE.get())) {
            return InteractionResult.PASS;
        }

        // Places like any block item: against the clicked face, or into a replaceable block
        BlockPlaceContext placeCtx = new BlockPlaceContext(ctx);
        BlockPos placePos = placeCtx.getClickedPos();
        BlockState toPlace = TradeBlocks.COIN_PILE.get().getStateForPlacement(placeCtx);
        if (!placeCtx.canPlace() || toPlace == null || !toPlace.canSurvive(level, placePos)) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!level.setBlock(placePos, toPlace, Block.UPDATE_ALL_IMMEDIATE)) return InteractionResult.FAIL;
        level.gameEvent(player, GameEvent.BLOCK_PLACE, placePos);

        if (level.getBlockEntity(placePos) instanceof CoinBlockEntity be) {
            be.setCurrency(Purse.of(denom, 1));
        }

        if (player == null || !player.getAbilities().instabuild) {
            ctx.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    /** The registered coin item of each denomination, found by {@link #denom()}; built on first use. */
    private static volatile Map<CoinDenomination, Item> byDenomination;

    public static Item forDenomination(CoinDenomination denomination) {
        Map<CoinDenomination, Item> map = byDenomination;
        if (map == null) {
            map = new EnumMap<>(CoinDenomination.class);
            for (var entry : TradeItems.ITEMS.getEntries()) {
                if (entry.get() instanceof CoinItem coin) map.put(coin.denom, coin);
            }
            byDenomination = map;
        }
        Item item = map.get(denomination);
        if (item == null) throw new IllegalArgumentException("no coin item for " + denomination);
        return item;
    }

    public static CoinDenomination getDenomination(Item item) {
        return item instanceof CoinItem coin ? coin.denom : CoinDenomination.NONE;
    }

    public static ItemStack createCurrencyItem(CoinDenomination denomination, int count) {
        return denomination == CoinDenomination.NONE ? ItemStack.EMPTY : new ItemStack(forDenomination(denomination), count);
    }

    public static boolean isCoinItem(Item item) {
        return item instanceof CoinItem;
    }
}
