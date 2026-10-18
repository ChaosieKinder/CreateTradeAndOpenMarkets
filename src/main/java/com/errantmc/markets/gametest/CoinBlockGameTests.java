package com.errantmc.markets.gametest;

import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.block.TradeBlocks;
import com.errantmc.markets.block.entity.CoinBlockEntity;
import com.errantmc.markets.component.CurrencyData;
import com.errantmc.markets.data.CoinDenomination;
import com.errantmc.markets.item.TradeItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ErrantTrade.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CoinBlockGameTests {
    private CoinBlockGameTests() {}

    /** Breaking the support must go through the real removal path, not a call to a drop helper. */
    @GameTest(template = GameTests.ARENA)
    public static void pileDropsItsCoinsWhenItsSupportBreaks(GameTestHelper helper) {
        BlockPos support = new BlockPos(4, 1, 4);
        BlockPos pile = support.above();
        helper.setBlock(support.below(), Blocks.STONE); // catches the drops
        helper.setBlock(support, Blocks.STONE);
        helper.setBlock(pile, TradeBlocks.COIN_BLOCK.get());
        if (!(helper.getBlockEntity(pile) instanceof CoinBlockEntity be)) {
            helper.fail("coin block has no block entity", pile);
            return;
        }
        be.setCurrency(CurrencyData.newCurrencyFromDenom(CoinDenomination.COPPER_BIT, 3));

        helper.destroyBlock(support);

        helper.succeedWhen(() -> {
            helper.assertBlockNotPresent(TradeBlocks.COIN_BLOCK.get(), pile);
            helper.assertItemEntityCountIs(TradeItems.COIN_COPPER_BIT.get(), pile, 2.0, 3);
        });
    }
}
