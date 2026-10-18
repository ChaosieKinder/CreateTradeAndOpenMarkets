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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
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
     * BACKLOG #3: merging a coin stack into a nearly full pile used to wrap the count negative and
     * consume the stack. It is now refused whole.
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

        assertCount(helper, "copper bits in the pile", Integer.MAX_VALUE - 1,
                be.getCurrency().getFrom(CoinDenomination.COPPER_BIT));
        assertTrue(helper, "the coin stack to still be held",
                player.getMainHandItem().is(TradeItems.COIN_STACK_ITEM.get()));
        helper.succeed();
    }

    /**
     * BACKLOG #5: clicking the side of a block used to replace whatever sat on top of it. The
     * coin now places against the clicked face, and here nothing supports that cell.
     */
    @GameTest(template = ARENA)
    public static void placingACoinNeverReplacesABlock(GameTestHelper helper) {
        BlockPos clicked = new BlockPos(4, 1, 4);
        helper.setBlock(clicked, Blocks.STONE);
        helper.setBlock(clicked.above(), Blocks.OAK_PLANKS);
        ServerPlayer player = survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TradeItems.COIN_COPPER_BIT.get(), 1));

        helper.useBlock(clicked, player, hitFace(helper, clicked, Direction.EAST));

        helper.assertBlockPresent(Blocks.OAK_PLANKS, clicked.above());
        helper.assertBlockNotPresent(TradeBlocks.COIN_BLOCK.get(), clicked.east());
        assertCount(helper, "coins still held", 1, player.getMainHandItem().getCount());
        helper.succeed();
    }

    /** The ordinary case still works: a coin on top of a block starts a pile of one. */
    @GameTest(template = ARENA)
    public static void placingACoinOnTopStartsAPile(GameTestHelper helper) {
        BlockPos clicked = new BlockPos(4, 1, 4);
        helper.setBlock(clicked, Blocks.STONE);
        ServerPlayer player = survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TradeItems.COIN_GOLD_MARK.get(), 2));

        helper.useBlock(clicked, player, hitFace(helper, clicked, Direction.UP));

        if (!(helper.getBlockEntity(clicked.above()) instanceof CoinBlockEntity be)) {
            helper.fail("no pile on top of the clicked block", clicked.above());
            return;
        }
        assertCount(helper, "gold marks in the pile", 1, be.getCurrency().getFrom(CoinDenomination.GOLD_MARK));
        assertCount(helper, "coins still held", 1, player.getMainHandItem().getCount());
        helper.succeed();
    }

    /**
     * BACKLOG #6: a coin stack used to place a pile where nothing supported it, and the next
     * neighbour update dropped the whole balance.
     */
    @GameTest(template = ARENA)
    public static void coinStackIsNotPlacedWithoutSupport(GameTestHelper helper) {
        BlockPos clicked = new BlockPos(4, 1, 4);
        helper.setBlock(clicked, Blocks.STONE);
        ServerPlayer player = survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND,
                coinStackOf(CurrencyData.newCurrencyFromDenom(CoinDenomination.COPPER_BIT, 3)));

        helper.useBlock(clicked, player, hitFace(helper, clicked, Direction.EAST));

        helper.assertBlockNotPresent(TradeBlocks.COIN_BLOCK.get(), clicked.east());
        assertTrue(helper, "the coin stack to still be held",
                player.getMainHandItem().is(TradeItems.COIN_STACK_ITEM.get()));
        helper.succeed();
    }

    /** The ordinary case still works: a coin stack on top of a block becomes a pile holding its balance. */
    @GameTest(template = ARENA)
    public static void coinStackOnTopBecomesAPile(GameTestHelper helper) {
        BlockPos clicked = new BlockPos(4, 1, 4);
        helper.setBlock(clicked, Blocks.STONE);
        ServerPlayer player = survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND,
                coinStackOf(CurrencyData.newCurrencyFromDenom(CoinDenomination.SILVER_BIT, 7)));

        helper.useBlock(clicked, player, hitFace(helper, clicked, Direction.UP));

        if (!(helper.getBlockEntity(clicked.above()) instanceof CoinBlockEntity be)) {
            helper.fail("no pile on top of the clicked block", clicked.above());
            return;
        }
        assertCount(helper, "silver bits in the pile", 7, be.getCurrency().getFrom(CoinDenomination.SILVER_BIT));
        assertTrue(helper, "the coin stack to be used up", player.getMainHandItem().isEmpty());
        helper.succeed();
    }

    // ---------- depositing ----------

    /**
     * A plain right-click with coins in hand deposits the whole held stack. (README says shift is
     * for a whole stack; the code has never distinguished.)
     */
    @GameTest(template = ARENA)
    public static void rightClickWithCoinsDepositsTheWholeHeldStack(GameTestHelper helper) {
        CoinBlockEntity be = placePile(helper, CurrencyData.newCurrencyFromDenom(CoinDenomination.COPPER_BIT, 1));
        if (be == null) return;
        ServerPlayer player = survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TradeItems.COIN_SILVER_BIT.get(), 12));

        helper.useBlock(PILE, player);

        assertCount(helper, "silver bits in the pile", 12, be.getCurrency().getFrom(CoinDenomination.SILVER_BIT));
        assertTrue(helper, "the held coins to be used up", player.getMainHandItem().isEmpty());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void aCoinStackMergesIntoAPile(GameTestHelper helper) {
        CoinBlockEntity be = placePile(helper, CurrencyData.newCurrencyFromDenom(CoinDenomination.COPPER_BIT, 2));
        if (be == null) return;
        ServerPlayer player = survivalPlayer(helper);
        CurrencyData stack = CurrencyData.newCurrencyFromDenom(CoinDenomination.COPPER_BIT, 5)
                .add(CoinDenomination.GOLD_MARK, 1).updated();
        player.setItemInHand(InteractionHand.MAIN_HAND, coinStackOf(stack));

        helper.useBlock(PILE, player);

        assertCount(helper, "copper bits in the pile", 7, be.getCurrency().getFrom(CoinDenomination.COPPER_BIT));
        assertCount(helper, "gold marks in the pile", 1, be.getCurrency().getFrom(CoinDenomination.GOLD_MARK));
        assertTrue(helper, "the coin stack to be used up", player.getMainHandItem().isEmpty());
        helper.succeed();
    }

    // ---------- withdrawing ----------

    @GameTest(template = ARENA)
    public static void leftClickTakesOneCoinOfTheHighestDenomination(GameTestHelper helper) {
        CurrencyData start = CurrencyData.newCurrencyFromDenom(CoinDenomination.COPPER_BIT, 3)
                .add(CoinDenomination.GOLD_MARK, 2).updated();
        CoinBlockEntity be = placePile(helper, start);
        if (be == null) return;
        ServerPlayer player = survivalPlayer(helper);

        boolean cancelled = leftClick(helper, player, PILE);

        assertTrue(helper, "the click to be cancelled so the pile is not mined", cancelled);
        assertCount(helper, "gold marks in the pile", 1, be.getCurrency().getFrom(CoinDenomination.GOLD_MARK));
        assertCount(helper, "copper bits in the pile", 3, be.getCurrency().getFrom(CoinDenomination.COPPER_BIT));
        helper.assertItemEntityCountIs(TradeItems.COIN_GOLD_MARK.get(), PILE, 2.0, 1);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void sneakLeftClickTakesUpToAStack(GameTestHelper helper) {
        CoinBlockEntity be = placePile(helper, CurrencyData.newCurrencyFromDenom(CoinDenomination.COPPER_SHARD, 100));
        if (be == null) return;
        ServerPlayer player = survivalPlayer(helper);
        player.setPose(Pose.CROUCHING);

        leftClick(helper, player, PILE);

        assertCount(helper, "copper shards left in the pile", 36, be.getCurrency().getFrom(CoinDenomination.COPPER_SHARD));
        helper.assertItemEntityCountIs(TradeItems.COIN_COPPER_SHARD.get(), PILE, 2.0, 64);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void takingTheLastCoinRemovesThePile(GameTestHelper helper) {
        helper.setBlock(PILE.below(2), Blocks.STONE);
        CoinBlockEntity be = placePile(helper, CurrencyData.newCurrencyFromDenom(CoinDenomination.EMERALD_SUN, 1));
        if (be == null) return;
        ServerPlayer player = survivalPlayer(helper);

        leftClick(helper, player, PILE);

        helper.assertBlockNotPresent(TradeBlocks.COIN_BLOCK.get(), PILE);
        helper.assertItemEntityCountIs(TradeItems.COIN_EMERALD_SUN.get(), PILE, 2.0, 1);
        helper.succeed();
    }

    // ---------- breaking ----------

    @GameTest(template = ARENA)
    public static void breakingASmallPileDropsTheCoinsThemselves(GameTestHelper helper) {
        helper.setBlock(PILE.below(2), Blocks.STONE);
        CurrencyData start = CurrencyData.newCurrencyFromDenom(CoinDenomination.COPPER_BIT, 130)
                .add(CoinDenomination.SILVER_CROWN, 3).updated();
        if (placePile(helper, start) == null) return;

        helper.destroyBlock(PILE);

        helper.succeedWhen(() -> {
            helper.assertItemEntityCountIs(TradeItems.COIN_COPPER_BIT.get(), PILE, 2.0, 130);
            helper.assertItemEntityCountIs(TradeItems.COIN_SILVER_CROWN.get(), PILE, 2.0, 3);
            helper.assertItemEntityNotPresent(TradeItems.COIN_STACK_ITEM.get(), PILE, 2.0);
        });
    }

    /** More than twenty stacks' worth drops as one coin stack, not hundreds of entities (README). */
    @GameTest(template = ARENA)
    public static void breakingABigPileDropsOneCoinStack(GameTestHelper helper) {
        helper.setBlock(PILE.below(2), Blocks.STONE);
        CurrencyData start = CurrencyData.newCurrencyFromDenom(CoinDenomination.COPPER_BIT, 21 * 64)
                .add(CoinDenomination.EMERALD_THRONE, 2).updated();
        if (placePile(helper, start) == null) return;

        helper.destroyBlock(PILE);

        helper.succeedWhen(() -> {
            helper.assertItemEntityNotPresent(TradeItems.COIN_COPPER_BIT.get(), PILE, 2.0);
            helper.assertItemEntityCountIs(TradeItems.COIN_STACK_ITEM.get(), PILE, 2.0, 1);
            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new AABB(helper.absolutePos(PILE)).inflate(2.0),
                    e -> e.getItem().is(TradeItems.COIN_STACK_ITEM.get()));
            if (!CurrencyData.getFrom(drops.getFirst().getItem()).equals(start)) {
                helper.fail("the coin stack should hold exactly what the pile held");
            }
        });
    }

    /** As with chests, a creative player breaking a pile gets nothing. */
    @GameTest(template = ARENA)
    public static void creativeBreakingDropsNothing(GameTestHelper helper) {
        helper.setBlock(PILE.below(2), Blocks.STONE);
        if (placePile(helper, CurrencyData.newCurrencyFromDenom(CoinDenomination.GOLD_REGAL, 5)) == null) return;
        ServerPlayer player = mockPlayer(helper, GameType.CREATIVE);

        player.gameMode.destroyBlock(helper.absolutePos(PILE));

        helper.assertBlockNotPresent(TradeBlocks.COIN_BLOCK.get(), PILE);
        helper.runAfterDelay(2, () -> {
            helper.assertItemEntityNotPresent(TradeItems.COIN_GOLD_REGAL.get(), PILE, 2.0);
            helper.succeed();
        });
    }

    // ---------- saving ----------

    /** The block entity's NBT is a save format: a pile must come back from disk unchanged. */
    @GameTest(template = ARENA)
    public static void aPileSurvivesSavingAndLoading(GameTestHelper helper) {
        CurrencyData start = CurrencyData.newCurrencyFromDenom(CoinDenomination.SILVER_BIT, 17)
                .add(CoinDenomination.EMERALD_THRONE, 3).updated();
        CoinBlockEntity be = placePile(helper, start);
        if (be == null) return;
        var registries = helper.getLevel().registryAccess();

        CompoundTag saved = be.saveWithoutMetadata(registries);
        CoinBlockEntity loaded = new CoinBlockEntity(be.getBlockPos(), be.getBlockState());
        loaded.loadWithComponents(saved, registries);

        if (!loaded.getCurrency().equals(start)) {
            helper.fail("loaded " + loaded.getCurrency() + ", saved " + start);
        }
        helper.succeed();
    }

    // ---------- helpers ----------

    /** Posts the event the server fires when a player starts mining; returns whether it was cancelled. */
    private static boolean leftClick(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
        var event = new PlayerInteractEvent.LeftClickBlock(player, helper.absolutePos(pos), Direction.UP,
                PlayerInteractEvent.LeftClickBlock.Action.START);
        NeoForge.EVENT_BUS.post(event);
        return event.isCanceled();
    }

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
