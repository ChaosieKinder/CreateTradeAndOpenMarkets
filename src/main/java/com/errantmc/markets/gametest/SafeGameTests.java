package com.errantmc.markets.gametest;

import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.block.TradeBlocks;
import com.errantmc.markets.block.entity.SafeBlockEntity;
import net.minecraft.core.BlockPos;
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
}
