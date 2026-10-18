package com.errantmc.markets.gametest;

import com.errantmc.markets.Config;
import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.component.Purses;
import com.errantmc.markets.domain.CoinDenomination;
import com.errantmc.markets.domain.Purse;
import com.errantmc.markets.item.PlayerMoney;
import com.errantmc.markets.item.TradeItems;
import com.errantmc.markets.npcinteractions.trading.TradeAttachments;
import com.errantmc.markets.npcinteractions.trading.VillagerBalance;
import com.errantmc.markets.screen.villager.VillagerInteractionMenu;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.errantmc.markets.gametest.GameTests.*;

/**
 * Villagers' tills (docs/design/economy.md §3). The rule under every test: a trade moves money
 * between the player and the villager, so their total is the same before and after.
 */
@GameTestHolder(ErrantTrade.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EconomyGameTests {
    private EconomyGameTests() {}

    private static final int BUY = 0, SELL = 1, BREAD = 0, WHEAT = 0;

    @GameTest(template = ARENA)
    public static void aNoviceStartsWithTheStartingBalance(GameTestHelper helper) {
        Villager farmer = VillagerInteractionGameTests.farmer(helper);
        assertCount(helper, "an untouched till", VillagerBalance.UNSET, farmer.getData(TradeAttachments.BALANCE));

        assertCount(helper, "a novice's till", Config.STARTING_BALANCE.get(), VillagerBalance.get(farmer));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void aMasterStartsRicher(GameTestHelper helper) {
        Villager farmer = VillagerInteractionGameTests.farmer(helper);
        farmer.setVillagerData(farmer.getVillagerData().setLevel(5));

        assertCount(helper, "a master's till", Config.STARTING_BALANCE.get() + 4 * Config.STARTING_BALANCE_PER_LEVEL.get(),
                VillagerBalance.get(farmer));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void buyingPaysTheVillagerAndConservesMoney(GameTestHelper helper) {
        Villager farmer = VillagerInteractionGameTests.farmer(helper);
        ServerPlayer player = survivalPlayer(helper);
        player.getInventory().setItem(3, new ItemStack(TradeItems.COIN_GOLD_MARK.get(), 1)); // pays 500 for a 100 item
        VillagerInteractionMenu menu = TradingGameTests.open(helper, player, farmer);
        if (menu == null) return;
        long tillBefore = VillagerBalance.get(farmer);
        long totalBefore = total(player, farmer);

        menu.clickMenuButton(player, VillagerInteractionMenu.tradeButton(BUY, BREAD));

        assertCount(helper, "bread", 6, TradingGameTests.count(player, Items.BREAD));
        assertCount(helper, "the till's gain", 100, VillagerBalance.get(farmer) - tillBefore);
        assertCount(helper, "money in total", totalBefore, total(player, farmer));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void sellingIsPaidFromTheTillAndConservesMoney(GameTestHelper helper) {
        Villager farmer = VillagerInteractionGameTests.farmer(helper);
        ServerPlayer player = survivalPlayer(helper);
        player.getInventory().setItem(3, new ItemStack(Items.WHEAT, 20));
        VillagerInteractionMenu menu = TradingGameTests.open(helper, player, farmer);
        if (menu == null) return;
        long tillBefore = VillagerBalance.get(farmer);
        long totalBefore = total(player, farmer);

        menu.clickMenuButton(player, VillagerInteractionMenu.tradeButton(SELL, WHEAT));

        assertCount(helper, "the till's loss", 100, tillBefore - VillagerBalance.get(farmer));
        assertCount(helper, "money in total", totalBefore, total(player, farmer));
        helper.succeed();
    }

    /** The author's example: a villager keeps buying while it has money, and stops when it runs out. */
    @GameTest(template = ARENA)
    public static void aVillagerBuysUntilItsTillRunsDry(GameTestHelper helper) {
        Villager farmer = VillagerInteractionGameTests.farmer(helper);
        farmer.setData(TradeAttachments.BALANCE, 250L);
        ServerPlayer player = survivalPlayer(helper);
        player.getInventory().setItem(3, new ItemStack(Items.WHEAT, 64));
        VillagerInteractionMenu menu = TradingGameTests.open(helper, player, farmer);
        if (menu == null) return;

        for (int i = 0; i < 3; i++) menu.clickMenuButton(player, VillagerInteractionMenu.tradeButton(SELL, WHEAT));

        assertCount(helper, "wheat sold (two bundles of 20)", 64 - 40, TradingGameTests.count(player, Items.WHEAT));
        assertCount(helper, "paid to the player", 200, PlayerMoney.loose(player).totalValue());
        assertCount(helper, "left in the till", 50, VillagerBalance.get(farmer));
        assertCount(helper, "the sell entry's stock (no cap)", -1, TradingGameTests.entry(menu, SELL, WHEAT).stock());
        helper.succeed();
    }

    /** The screen shows the till; the client's copy follows the server's after a trade. */
    @GameTest(template = ARENA)
    public static void theMenuReportsTheTill(GameTestHelper helper) {
        Villager farmer = VillagerInteractionGameTests.farmer(helper);
        ServerPlayer player = survivalPlayer(helper);
        ItemStack wallet = new ItemStack(TradeItems.LEATHER_WALLET.get());
        Purses.set(wallet, Purse.of(CoinDenomination.SILVER_CROWN, 2));
        player.getInventory().setItem(8, wallet);
        VillagerInteractionMenu menu = TradingGameTests.open(helper, player, farmer);
        if (menu == null) return;
        long before = menu.getVillagerBalance();

        menu.clickMenuButton(player, VillagerInteractionMenu.tradeButton(BUY, BREAD));

        assertCount(helper, "the till the menu reports", before + 100, menu.getVillagerBalance());
        helper.succeed();
    }

    /** All the money the player and the villager hold: coins, wallet and till. */
    static long total(ServerPlayer player, Villager villager) {
        return PlayerMoney.loose(player).totalValue() + PlayerMoney.wallet(player).totalValue() + VillagerBalance.get(villager);
    }
}
