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
        if (item == TAOMItems.COIN_COPPER_BIT.get())   return CoinDenomination.COPPER_BIT;
        if (item == TAOMItems.COIN_COPPER_SHARD.get())   return CoinDenomination.COPPER_SHARD;
        if (item == TAOMItems.COIN_SILVER_BIT.get())   return CoinDenomination.SILVER_BIT;
        if (item == TAOMItems.COIN_SILVER_CROWN.get()) return CoinDenomination.SILVER_CROWN;
        if (item == TAOMItems.COIN_GOLD_MARK.get()) return CoinDenomination.GOLD_MARK;
        if (item == TAOMItems.COIN_GOLD_REGAL.get()) return CoinDenomination.GOLD_REGAL;
        if (item == TAOMItems.COIN_EMERALD_SUN.get())  return CoinDenomination.EMERALD_SUN;
        if (item == TAOMItems.COIN_EMERALD_THRONE.get())  return CoinDenomination.EMERALD_THRONE;
        return CoinDenomination.NONE;
    }

    public static ItemStack createCurrencyItem(CoinDenomination denomination, int value ) {
        if (denomination == CoinDenomination.COPPER_BIT) return new ItemStack(TAOMItems.COIN_COPPER_BIT.get(), value);
        if (denomination == CoinDenomination.COPPER_SHARD) return new ItemStack(TAOMItems.COIN_COPPER_SHARD.get(), value);
        if (denomination == CoinDenomination.SILVER_BIT) return new ItemStack(TAOMItems.COIN_SILVER_BIT.get(), value);
        if (denomination == CoinDenomination.SILVER_CROWN) return new ItemStack(TAOMItems.COIN_SILVER_CROWN.get(), value);
        if (denomination == CoinDenomination.GOLD_MARK) return new ItemStack(TAOMItems.COIN_GOLD_MARK.get(), value);
        if (denomination == CoinDenomination.GOLD_REGAL) return new ItemStack(TAOMItems.COIN_GOLD_REGAL.get(), value);
        if (denomination == CoinDenomination.EMERALD_SUN) return new ItemStack(TAOMItems.COIN_EMERALD_SUN.get(), value);
        if (denomination == CoinDenomination.EMERALD_THRONE) return new ItemStack(TAOMItems.COIN_EMERALD_THRONE.get(), value);
        return ItemStack.EMPTY;
    }

    public static boolean isCoinItem(Item item) {
        return item == TAOMItems.COIN_COPPER_SHARD.get()
                || item == TAOMItems.COIN_COPPER_BIT.get()
                || item == TAOMItems.COIN_SILVER_BIT.get()
                || item == TAOMItems.COIN_SILVER_CROWN.get()
                || item == TAOMItems.COIN_GOLD_MARK.get()
                || item == TAOMItems.COIN_GOLD_REGAL.get()
                || item == TAOMItems.COIN_EMERALD_SUN.get()
                || item == TAOMItems.COIN_EMERALD_THRONE.get();
    }
}
