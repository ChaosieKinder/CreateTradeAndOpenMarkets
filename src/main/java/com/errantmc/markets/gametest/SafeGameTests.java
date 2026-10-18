package com.errantmc.markets.gametest;

import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.block.TradeBlocks;
import com.errantmc.markets.block.entity.SafeBlockEntity;
import com.errantmc.markets.item.TradeItems;
import com.errantmc.markets.screen.safe.SafeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.errantmc.markets.gametest.GameTests.*;

@GameTestHolder(ErrantTrade.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SafeGameTests {
    private SafeGameTests() {}

    private static final BlockPos SAFE = new BlockPos(4, 1, 4);

    /** BACKLOG #7: clearContents used to clear slot 0 once per slot and leave the rest. */
    @GameTest(template = ARENA)
    public static void clearContentsEmptiesEverySlot(GameTestHelper helper) {
        helper.setBlock(SAFE, TradeBlocks.SAFE_BLOCK.get());
        if (!(helper.getBlockEntity(SAFE) instanceof SafeBlockEntity safe)) {
            helper.fail("safe has no block entity", SAFE);
            return;
        }
        for (int slot = 0; slot < safe.inventory.getSlots(); slot++) {
            safe.inventory.setStackInSlot(slot, new ItemStack(Items.GOLD_INGOT));
        }

        safe.clearContents();

        for (int slot = 0; slot < safe.inventory.getSlots(); slot++) {
            assertTrue(helper, "safe slot " + slot + " to be empty", safe.inventory.getStackInSlot(slot).isEmpty());
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void breakingASafeDropsItsContents(GameTestHelper helper) {
        helper.setBlock(SAFE.below(), net.minecraft.world.level.block.Blocks.STONE);
        SafeBlockEntity safe = placeSafe(helper);
        if (safe == null) return;
        safe.inventory.setStackInSlot(0, new ItemStack(Items.GOLD_INGOT, 20));
        safe.inventory.setStackInSlot(24, new ItemStack(TradeItems.COIN_GOLD_MARK.get(), 7));

        helper.destroyBlock(SAFE);

        helper.succeedWhen(() -> {
            helper.assertItemEntityCountIs(Items.GOLD_INGOT, SAFE, 2.0, 20);
            helper.assertItemEntityCountIs(TradeItems.COIN_GOLD_MARK.get(), SAFE, 2.0, 7);
        });
    }

    /** Each of the 25 slots holds at most 25 of anything. */
    @GameTest(template = ARENA)
    public static void aSafeSlotHoldsAtMostTwentyFive(GameTestHelper helper) {
        SafeBlockEntity safe = placeSafe(helper);
        if (safe == null) return;

        ItemStack rest = safe.inventory.insertItem(3, new ItemStack(Items.IRON_INGOT, 64), false);

        assertCount(helper, "ingots in the slot", 25, safe.inventory.getStackInSlot(3).getCount());
        assertCount(helper, "ingots refused", 39, rest.getCount());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void aPlacedSafeFacesThePlayer(GameTestHelper helper) {
        helper.setBlock(SAFE, net.minecraft.world.level.block.Blocks.STONE);
        ServerPlayer player = survivalPlayer(helper);
        player.setYRot(0); // looking south
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TradeBlocks.SAFE_BLOCK.get()));

        helper.useBlock(SAFE, player, hitFace(helper, SAFE, Direction.UP));

        helper.assertBlockProperty(SAFE.above(), HorizontalDirectionalBlock.FACING, Direction.NORTH);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void usingASafeOpensItsMenu(GameTestHelper helper) {
        if (placeSafe(helper) == null) return;
        ServerPlayer player = survivalPlayer(helper);

        helper.useBlock(SAFE, player);

        assertTrue(helper, "the safe's menu to be open", player.containerMenu instanceof SafeMenu);
        helper.succeed();
    }

    private static SafeBlockEntity placeSafe(GameTestHelper helper) {
        helper.setBlock(SAFE, TradeBlocks.SAFE_BLOCK.get());
        if (!(helper.getBlockEntity(SAFE) instanceof SafeBlockEntity safe)) {
            helper.fail("safe has no block entity", SAFE);
            return null;
        }
        return safe;
    }
}
