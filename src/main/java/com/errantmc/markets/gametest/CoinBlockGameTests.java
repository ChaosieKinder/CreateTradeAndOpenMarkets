package com.errantmc.markets.gametest;

import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.block.TradeBlocks;
import com.errantmc.markets.block.entity.CoinBlockEntity;
import com.errantmc.markets.component.CurrencyData;
import com.errantmc.markets.data.CoinDenomination;
import com.errantmc.markets.item.CoinItem;
import com.errantmc.markets.item.TradeItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ErrantTrade.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CoinBlockGameTests {
    private CoinBlockGameTests() {}

    private static final BlockPos PILE = new BlockPos(4, 2, 4);

    /** Breaking the support must go through the real removal path, not a call to a drop helper. */
    @GameTest(template = GameTests.ARENA)
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
    @GameTest(template = GameTests.ARENA)
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

    /** The helper's mock player is creative, and creative inventories are never charged. */
    private static ServerPlayer survivalPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static int coinsIn(ServerPlayer player) {
        var inv = player.getInventory();
        int total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (CoinItem.isCoinItem(s.getItem())) total += s.getCount();
        }
        return total;
    }

    private static void assertCount(GameTestHelper helper, String what, int expected, int actual) {
        if (expected != actual) helper.fail("expected " + expected + " " + what + ", found " + actual);
    }
}
