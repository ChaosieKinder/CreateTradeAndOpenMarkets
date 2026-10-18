package com.errantmc.markets.gametest;

import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.component.CurrencyData;
import com.errantmc.markets.component.WalletBalance;
import com.errantmc.markets.data.CoinDenomination;
import com.errantmc.markets.item.TradeItems;
import com.errantmc.markets.screen.wallet.WalletMenu;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.errantmc.markets.gametest.GameTests.*;

@GameTestHolder(ErrantTrade.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WalletGameTests {
    private WalletGameTests() {}

    /**
     * BUG (BACKLOG #2), pinned as it behaves today: "deposit all" passes a hotbar index where a
     * menu slot index belongs, so a coin stack in hotbar slot 2 moves wallet slot 2 out instead.
     */
    @GameTest(template = ARENA)
    public static void depositAllTakesCoinStacksAndLeavesTheWalletsContents(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TradeItems.WALLET_LEATHER_ITEM.get()));
        player.getInventory().setItem(2, coinStackOf(CurrencyData.newCurrencyFromDenom(CoinDenomination.COPPER_BIT, 5)));
        WalletMenu menu = new WalletMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);
        menu.getSlot(2).set(new ItemStack(TradeItems.ID_CARD.get()));

        menu.clickMenuButton(player, WalletMenu.BTN_DEPOSIT_ALL);

        assertTrue(helper, "wallet slot 2 to be emptied", menu.getSlot(2).getItem().isEmpty());
        assertTrue(helper, "the coin stack to stay in the hotbar",
                player.getInventory().getItem(2).is(TradeItems.COIN_STACK_ITEM.get()));
        assertCount(helper, "copper bits in the wallet", 0,
                WalletBalance.get(menu.getWalletStack()).getFrom(CoinDenomination.COPPER_BIT));
        helper.succeed();
    }
}
