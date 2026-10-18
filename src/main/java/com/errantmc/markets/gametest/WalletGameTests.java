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
     * BACKLOG #2: "deposit all" used to pass a hotbar index where a menu slot index belongs, so a
     * coin stack in hotbar slot 2 moved wallet slot 2 out instead.
     */
    @GameTest(template = ARENA)
    public static void depositAllTakesCoinStacksAndLeavesTheWalletsContents(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TradeItems.WALLET_LEATHER_ITEM.get()));
        player.getInventory().setItem(2, coinStackOf(CurrencyData.newCurrencyFromDenom(CoinDenomination.COPPER_BIT, 5)));
        player.setItemInHand(InteractionHand.OFF_HAND,
                coinStackOf(CurrencyData.newCurrencyFromDenom(CoinDenomination.GOLD_MARK, 2)));
        player.getInventory().setItem(20, new ItemStack(TradeItems.COIN_SILVER_BIT.get(), 6));
        WalletMenu menu = new WalletMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);
        menu.getSlot(0).set(new ItemStack(TradeItems.ID_CARD.get()));
        menu.getSlot(2).set(new ItemStack(TradeItems.ID_CARD.get()));

        menu.clickMenuButton(player, WalletMenu.BTN_DEPOSIT_ALL);

        assertTrue(helper, "wallet slot 0 to keep its card", menu.getSlot(0).getItem().is(TradeItems.ID_CARD.get()));
        assertTrue(helper, "wallet slot 2 to keep its card", menu.getSlot(2).getItem().is(TradeItems.ID_CARD.get()));
        assertTrue(helper, "the hotbar coin stack to be taken", player.getInventory().getItem(2).isEmpty());
        assertTrue(helper, "the offhand coin stack to be taken", player.getOffhandItem().isEmpty());
        CurrencyData balance = WalletBalance.get(menu.getWalletStack());
        assertCount(helper, "copper bits in the wallet", 5, balance.getFrom(CoinDenomination.COPPER_BIT));
        assertCount(helper, "gold marks in the wallet", 2, balance.getFrom(CoinDenomination.GOLD_MARK));
        assertCount(helper, "silver bits in the wallet", 6, balance.getFrom(CoinDenomination.SILVER_BIT));
        assertCount(helper, "coins left in the inventory", 0, coinsIn(player));
        helper.succeed();
    }

    /** BACKLOG #3: what does not fit under Integer.MAX_VALUE stays in the inventory. */
    @GameTest(template = ARENA)
    public static void depositAllIntoANearlyFullWalletLeavesTheRest(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack wallet = new ItemStack(TradeItems.WALLET_LEATHER_ITEM.get());
        WalletBalance.set(wallet, CurrencyData.newCurrencyFromDenom(CoinDenomination.COPPER_BIT, Integer.MAX_VALUE - 1));
        player.setItemInHand(InteractionHand.MAIN_HAND, wallet);
        player.getInventory().setItem(5, new ItemStack(TradeItems.COIN_COPPER_BIT.get(), 5));
        WalletMenu menu = new WalletMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);

        menu.clickMenuButton(player, WalletMenu.BTN_DEPOSIT_ALL);

        assertCount(helper, "copper bits in the wallet", Integer.MAX_VALUE,
                WalletBalance.get(menu.getWalletStack()).getFrom(CoinDenomination.COPPER_BIT));
        assertCount(helper, "coins left in the inventory", 4, coinsIn(player));
        helper.succeed();
    }
}
