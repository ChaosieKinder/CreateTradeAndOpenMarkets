package com.errantmc.markets.gametest;

import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.block.TradeBlocks;
import com.errantmc.markets.block.entity.CoinBlockEntity;
import com.errantmc.markets.component.CurrencyData;
import com.errantmc.markets.data.CoinDenomination;
import com.errantmc.markets.item.TradeItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.errantmc.markets.gametest.GameTests.*;

@GameTestHolder(ErrantTrade.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CoinBlockGameTests {
    private CoinBlockGameTests() {}

    private static final BlockPos PILE = new BlockPos(4, 2, 4);

    /** Breaking the support must go through the real removal path, not a call to a drop helper. */
    @GameTest(template = ARENA)
    public static void pileDropsItsCoinsWhenItsSupportBreaks(GameTestHelper helper) {
        helper.setBlock(PILE.below(2), Blocks.STONE); // catches the drops
        CoinBlockEntity be = placePile(helper, CurrencyData.newCurrencyFromDenom(CoinDenomination.COPPER_BIT, 3));
        if (be == null) return;

        helper.destroyBlock(PILE.below());

        helper.succeedWhen(() -> {
            helper.assertBlockNotPresent(TradeBlocks.COIN_BLOCK.get(), PILE);
            helper.assertItemEntityCountIs(TradeItems.COIN_COPPER_BIT.get(), PILE, 2.0, 3);
        });
    }

    /** Every coin stack, main inventory and offhand, lands in the pile exactly once (BACKLOG #1). */
    @GameTest(template = ARENA)
    public static void sneakDoubleTapDepositsEveryCoinInTheInventory(GameTestHelper helper) {
        CoinBlockEntity be = placePile(helper, CurrencyData.newCurrencyFromDenom(CoinDenomination.COPPER_BIT, 1));
        if (be == null) return;
        ServerPlayer player = survivalPlayer(helper);
        player.getInventory().setItem(5, new ItemStack(TradeItems.COIN_COPPER_BIT.get(), 10));
        player.getInventory().setItem(9, new ItemStack(TradeItems.COIN_SILVER_CROWN.get(), 2));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(TradeItems.COIN_COPPER_SHARD.get(), 3));
        player.setPose(Pose.CROUCHING);

        helper.useBlock(PILE, player);
        helper.useBlock(PILE, player);

        CurrencyData pile = be.getCurrency();
        assertCount(helper, "copper bits in the pile", 11, pile.getFrom(CoinDenomination.COPPER_BIT));
        assertCount(helper, "silver crowns in the pile", 2, pile.getFrom(CoinDenomination.SILVER_CROWN));
        assertCount(helper, "copper shards in the pile", 3, pile.getFrom(CoinDenomination.COPPER_SHARD));
        assertCount(helper, "coins left in the inventory", 0, coinsIn(player));
        helper.succeed();
    }

    /**
     * BUG (BACKLOG #3), pinned as it behaves today: merging a coin stack into a nearly full pile
     * wraps the count negative and consumes the stack.
     */
    @GameTest(template = ARENA)
    public static void coinStackIntoAFullPileIsRefused(GameTestHelper helper) {
        CoinBlockEntity be = placePile(helper,
                CurrencyData.newCurrencyFromDenom(CoinDenomination.COPPER_BIT, Integer.MAX_VALUE - 1));
        if (be == null) return;
        ServerPlayer player = survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND,
                coinStackOf(CurrencyData.newCurrencyFromDenom(CoinDenomination.COPPER_BIT, 5)));

        helper.useBlock(PILE, player);

        assertCount(helper, "copper bits in the pile", Integer.MIN_VALUE + 3,
                be.getCurrency().getFrom(CoinDenomination.COPPER_BIT));
        assertTrue(helper, "the coin stack to be consumed", player.getMainHandItem().isEmpty());
        helper.succeed();
    }

    /**
     * BUG (BACKLOG #5), pinned as it behaves today: clicking the side of a block with a coin
     * replaces whatever sits on top of that block.
     */
    @GameTest(template = ARENA)
    public static void placingACoinNeverReplacesABlock(GameTestHelper helper) {
        BlockPos clicked = new BlockPos(4, 1, 4);
        helper.setBlock(clicked, Blocks.STONE);
        helper.setBlock(clicked.above(), Blocks.OAK_PLANKS);
        ServerPlayer player = survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TradeItems.COIN_COPPER_BIT.get(), 1));

        helper.useBlock(clicked, player, hitFace(helper, clicked, Direction.EAST));

        helper.assertBlockPresent(TradeBlocks.COIN_BLOCK.get(), clicked.above());
        assertTrue(helper, "the coin to be consumed", player.getMainHandItem().isEmpty());
        helper.succeed();
    }

    /**
     * BUG (BACKLOG #6), pinned as it behaves today: a coin stack places a pile where nothing
     * supports it, and the next neighbour update would drop the whole balance.
     */
    @GameTest(template = ARENA)
    public static void coinStackIsNotPlacedWithoutSupport(GameTestHelper helper) {
        BlockPos clicked = new BlockPos(4, 1, 4);
        helper.setBlock(clicked, Blocks.STONE);
        ServerPlayer player = survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND,
                coinStackOf(CurrencyData.newCurrencyFromDenom(CoinDenomination.COPPER_BIT, 3)));

        helper.useBlock(clicked, player, hitFace(helper, clicked, Direction.EAST));

        helper.assertBlockPresent(TradeBlocks.COIN_BLOCK.get(), clicked.east());
        assertTrue(helper, "the coin stack to be consumed", player.getMainHandItem().isEmpty());
        helper.succeed();
    }

    // ---------- helpers ----------

    /** A pile at {@link #PILE} on a stone support, holding {@code currency}; null after failing the test. */
    private static CoinBlockEntity placePile(GameTestHelper helper, CurrencyData currency) {
        helper.setBlock(PILE.below(), Blocks.STONE);
        helper.setBlock(PILE, TradeBlocks.COIN_BLOCK.get());
        if (!(helper.getBlockEntity(PILE) instanceof CoinBlockEntity be)) {
            helper.fail("coin block has no block entity", PILE);
            return null;
        }
        be.setCurrency(currency);
        return be;
    }
}
