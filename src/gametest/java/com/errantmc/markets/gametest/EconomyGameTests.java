package com.errantmc.markets.gametest;

import com.errantmc.markets.Config;
import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.component.Purses;
import com.errantmc.markets.domain.CoinDenomination;
import com.errantmc.markets.domain.Purse;
import com.errantmc.markets.economy.MoneyLedger;
import com.errantmc.markets.economy.MoneySupply;
import com.errantmc.markets.economy.Treasuries;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.errantmc.markets.economy.VillageEconomy;
import com.errantmc.markets.item.PlayerMoney;
import com.errantmc.markets.item.TradeItems;
import com.errantmc.markets.npcinteractions.trading.TradeAttachments;
import com.errantmc.markets.npcinteractions.trading.VillagerBalance;
import com.errantmc.markets.screen.villager.VillagerInteractionMenu;
import com.errantmc.markets.villager.TradeVillagers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
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

    // ---------- the village treasury (economy step 2) ----------

    @GameTest(template = ARENA)
    public static void aVillagerInAVillagePaysTheFeeOnASale(GameTestHelper helper) {
        Villager farmer = VillagerInteractionGameTests.farmer(helper);
        GlobalPos village = joinVillage(helper, farmer);
        ServerPlayer player = survivalPlayer(helper);
        player.getInventory().setItem(3, new ItemStack(TradeItems.COIN_SILVER_CROWN.get(), 1));
        VillagerInteractionMenu menu = TradingGameTests.open(helper, player, farmer);
        if (menu == null) return;
        long till = VillagerBalance.get(farmer), treasury = treasury(helper, village);
        long totalBefore = total(player, farmer) + treasury;

        menu.clickMenuButton(player, VillagerInteractionMenu.tradeButton(BUY, BREAD));

        long fee = 100 * Config.TRADE_FEE_PERCENT.get() / 100;
        assertCount(helper, "the till's gain, less the fee", 100 - fee, VillagerBalance.get(farmer) - till);
        assertCount(helper, "the treasury's fee", fee, treasury(helper, village) - treasury);
        assertCount(helper, "money in total", totalBefore, total(player, farmer) + treasury(helper, village));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void aVillagerInAVillagePaysTheFeeWhenItBuys(GameTestHelper helper) {
        Villager farmer = VillagerInteractionGameTests.farmer(helper);
        GlobalPos village = joinVillage(helper, farmer);
        ServerPlayer player = survivalPlayer(helper);
        player.getInventory().setItem(3, new ItemStack(Items.WHEAT, 20));
        VillagerInteractionMenu menu = TradingGameTests.open(helper, player, farmer);
        if (menu == null) return;
        long till = VillagerBalance.get(farmer), treasury = treasury(helper, village);
        long totalBefore = total(player, farmer) + treasury;

        menu.clickMenuButton(player, VillagerInteractionMenu.tradeButton(SELL, WHEAT));

        long fee = 100 * Config.TRADE_FEE_PERCENT.get() / 100;
        assertCount(helper, "paid to the player", 100, PlayerMoney.loose(player).totalValue());
        assertCount(helper, "the till's loss, with the fee", 100 + fee, till - VillagerBalance.get(farmer));
        assertCount(helper, "money in total", totalBefore, total(player, farmer) + treasury(helper, village));
        helper.succeed();
    }

    /** Selling to a villager in a village costs it the price and the fee; it refuses if it has only the price. */
    @GameTest(template = ARENA)
    public static void theFeeCanLeaveAVillagerShort(GameTestHelper helper) {
        Villager farmer = VillagerInteractionGameTests.farmer(helper);
        joinVillage(helper, farmer);
        farmer.setData(TradeAttachments.BALANCE, 100L);
        ServerPlayer player = survivalPlayer(helper);
        player.getInventory().setItem(3, new ItemStack(Items.WHEAT, 20));
        VillagerInteractionMenu menu = TradingGameTests.open(helper, player, farmer);
        if (menu == null) return;

        menu.clickMenuButton(player, VillagerInteractionMenu.tradeButton(SELL, WHEAT));

        if (Config.TRADE_FEE_PERCENT.get() > 0) {
            assertCount(helper, "wheat kept", 20, TradingGameTests.count(player, Items.WHEAT));
            assertCount(helper, "the till", 100, VillagerBalance.get(farmer));
        }
        helper.succeed();
    }

    /** A village's banker trades from the treasury, and exchanging coins for coins makes no money. */
    @GameTest(template = ARENA)
    public static void aVillageBankerTradesFromTheTreasury(GameTestHelper helper) {
        Villager banker = banker(helper);
        GlobalPos village = joinVillage(helper, banker);
        ServerPlayer player = survivalPlayer(helper);
        player.getInventory().setItem(3, new ItemStack(TradeItems.COIN_SILVER_CROWN.get(), 5)); // exactly 500, no change
        VillagerInteractionMenu menu = TradingGameTests.open(helper, player, banker);
        if (menu == null) return;
        if (!menu.tillIsTreasury()) helper.fail("the banker's till should be the treasury");
        long treasury = treasury(helper, village);
        long tillBefore = VillagerBalance.get(banker);
        long playerBefore = playerMoney(player);

        menu.clickMenuButton(player, VillagerInteractionMenu.tradeButton(BUY, GOLD_MARK));

        assertCount(helper, "gold marks bought", 1, TradingGameTests.count(player, TradeItems.COIN_GOLD_MARK.get()));
        assertCount(helper, "the player's money", playerBefore, playerMoney(player));
        assertCount(helper, "the treasury (money for money)", treasury, treasury(helper, village));
        assertCount(helper, "the banker's own till, untouched", tillBefore, VillagerBalance.get(banker));
        assertCount(helper, "what the menu shows", treasury, menu.getVillagerBalance());
        helper.succeed();
    }

    /** Out of a village, the banker uses its own till, and the exchange still makes no money. */
    @GameTest(template = ARENA)
    public static void aBankerOutsideAVillageExchangesFromItsTill(GameTestHelper helper) {
        Villager banker = banker(helper);
        ServerPlayer player = survivalPlayer(helper);
        player.getInventory().setItem(3, new ItemStack(TradeItems.COIN_COPPER_BIT.get(), 1));
        VillagerInteractionMenu menu = TradingGameTests.open(helper, player, banker);
        if (menu == null) return;
        if (menu.tillIsTreasury()) helper.fail("a banker in no village has no treasury");
        long totalBefore = total(player, banker);

        menu.clickMenuButton(player, VillagerInteractionMenu.tradeButton(SELL, COPPER_BIT));

        assertCount(helper, "money in total", totalBefore, total(player, banker));
        helper.succeed();
    }

    // ---------- the daily settlement (economy step 3) ----------

    @GameTest(template = ARENA)
    public static void aVillagersFirstSettlementOnlyStartsTheClock(GameTestHelper helper) {
        Villager farmer = VillagerInteractionGameTests.farmer(helper);
        joinVillage(helper, farmer);
        long till = VillagerBalance.get(farmer);

        VillageEconomy.settle(farmer);

        assertCount(helper, "the settled day", today(helper), farmer.getData(TradeAttachments.SETTLED_DAY));
        assertCount(helper, "the till", till, VillagerBalance.get(farmer));
        helper.succeed();
    }

    /** Opening the shop the next day pays the wage from the treasury, which earned its day's income. */
    @GameTest(template = ARENA)
    public static void theNextDaysShopPaysTheWage(GameTestHelper helper) {
        Villager farmer = VillagerInteractionGameTests.farmer(helper);
        GlobalPos village = joinVillage(helper, farmer);
        VillagerBalance.set(farmer, 1000);
        Treasuries.get(helper.getLevel().getServer()).set(village, 5000);
        farmer.setData(TradeAttachments.SETTLED_DAY, today(helper) - 1);

        VillagerInteractionMenu menu = TradingGameTests.open(helper, survivalPlayer(helper), farmer);
        if (menu == null) return;

        long wage = Config.WAGE.get(), income = Config.TREASURY_INCOME_PER_VILLAGER.get();
        assertCount(helper, "the till, with a day's wage", 1000 + wage, VillagerBalance.get(farmer));
        assertCount(helper, "the treasury, with its income, less the wage", 5000 + income - wage, treasury(helper, village));
        assertCount(helper, "what the shop shows", 1000 + wage, menu.getVillagerBalance());
        assertCount(helper, "the settled day", today(helper), farmer.getData(TradeAttachments.SETTLED_DAY));
        helper.succeed();
    }

    /** A month away settles only the configured most days. */
    @GameTest(template = ARENA)
    public static void settlementIsCapped(GameTestHelper helper) {
        Villager farmer = VillagerInteractionGameTests.farmer(helper);
        GlobalPos village = joinVillage(helper, farmer);
        VillagerBalance.set(farmer, 0);
        Treasuries.get(helper.getLevel().getServer()).set(village, 50_000);
        farmer.setData(TradeAttachments.SETTLED_DAY, today(helper) - 30);

        VillageEconomy.settle(farmer);

        long days = Config.MAX_SETTLEMENT_DAYS.get();
        if (days * Config.WAGE.get() <= Config.UPKEEP_FLOOR.get()) { // below the floor, no upkeep muddies the sum
            assertCount(helper, "wages for the capped days", days * Config.WAGE.get(), VillagerBalance.get(farmer));
        }
        helper.succeed();
    }

    /** What a villager sells costs it to restock, paid at the next day's settlement. */
    @GameTest(template = ARENA)
    public static void sellingToAPlayerIsPaidForAtTheNextSettlement(GameTestHelper helper) {
        Villager farmer = VillagerInteractionGameTests.farmer(helper); // in no village: no wage
        ServerPlayer player = survivalPlayer(helper);
        player.getInventory().setItem(3, new ItemStack(TradeItems.COIN_SILVER_CROWN.get(), 1));
        VillagerInteractionMenu menu = TradingGameTests.open(helper, player, farmer);
        if (menu == null) return;

        menu.clickMenuButton(player, VillagerInteractionMenu.tradeButton(BUY, BREAD));

        long cost = 100 * Config.RESTOCK_COST_PERCENT.get() / 100;
        assertCount(helper, "restocking owed", cost, farmer.getData(TradeAttachments.RESTOCK_OWED));
        long till = VillagerBalance.get(farmer);
        farmer.setData(TradeAttachments.SETTLED_DAY, today(helper) - 1);
        VillageEconomy.settle(farmer);
        assertCount(helper, "the till, less the restocking", till - cost, VillagerBalance.get(farmer));
        assertCount(helper, "restocking owed after paying", 0, farmer.getData(TradeAttachments.RESTOCK_OWED));
        helper.succeed();
    }

    /** The banker's coins are money already: selling them owes no restocking. */
    @GameTest(template = ARENA)
    public static void exchangingCoinsOwesNoRestocking(GameTestHelper helper) {
        Villager banker = banker(helper);
        ServerPlayer player = survivalPlayer(helper);
        player.getInventory().setItem(3, new ItemStack(TradeItems.COIN_SILVER_CROWN.get(), 5));
        VillagerInteractionMenu menu = TradingGameTests.open(helper, player, banker);
        if (menu == null) return;

        menu.clickMenuButton(player, VillagerInteractionMenu.tradeButton(BUY, GOLD_MARK));

        assertCount(helper, "gold marks bought", 1, TradingGameTests.count(player, TradeItems.COIN_GOLD_MARK.get()));
        assertCount(helper, "restocking owed", 0, banker.getData(TradeAttachments.RESTOCK_OWED));
        helper.succeed();
    }

    // ---------- measuring the money supply (economy step 4) ----------

    @GameTest(template = ARENA)
    public static void aTradeLeavesTheMoneySupplyUnchanged(GameTestHelper helper) {
        Villager farmer = VillagerInteractionGameTests.farmer(helper);
        VillagerBalance.get(farmer); // its starting balance is new money; that is the next test's
        ServerPlayer player = survivalPlayer(helper);
        player.getInventory().setItem(3, new ItemStack(TradeItems.COIN_SILVER_CROWN.get(), 1));
        VillagerInteractionMenu menu = TradingGameTests.open(helper, player, farmer);
        if (menu == null) return;
        MinecraftServer server = helper.getLevel().getServer();
        long before = MoneySupply.measure(server).total();

        menu.clickMenuButton(player, VillagerInteractionMenu.tradeButton(BUY, BREAD));

        assertCount(helper, "the money supply", before, MoneySupply.measure(server).total());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void aStartingBalanceIsNewMoneyAndTheLedgerSaysSo(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Villager farmer = VillagerInteractionGameTests.farmer(helper);
        long supply = MoneySupply.measure(server).total(), created = MoneyLedger.get(server).created();

        long starting = VillagerBalance.get(farmer);

        assertCount(helper, "the money supply's growth", starting, MoneySupply.measure(server).total() - supply);
        assertCount(helper, "recorded as created", starting, MoneyLedger.get(server).created() - created);
        helper.succeed();
    }

    /** The settlement's change to the money supply is exactly what the ledger records it created and destroyed. */
    @GameTest(template = ARENA)
    public static void theLedgerExplainsASettlement(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Villager farmer = VillagerInteractionGameTests.farmer(helper);
        GlobalPos village = joinVillage(helper, farmer);
        VillagerBalance.set(farmer, 1000);
        Treasuries.get(server).set(village, 5000);
        farmer.setData(TradeAttachments.RESTOCK_OWED, 50L);
        farmer.setData(TradeAttachments.SETTLED_DAY, today(helper) - 2);
        MoneyLedger ledger = MoneyLedger.get(server);
        long supply = MoneySupply.measure(server).total(), created = ledger.created(), destroyed = ledger.destroyed();

        VillageEconomy.settle(farmer);

        long madeNow = ledger.created() - created, lostNow = ledger.destroyed() - destroyed;
        if (madeNow <= 0 || lostNow <= 0) helper.fail("expected income and restocking, got " + madeNow + " and " + lostNow);
        assertCount(helper, "the money supply's change", madeNow - lostNow, MoneySupply.measure(server).total() - supply);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void theMoneyCommandRuns(GameTestHelper helper) throws CommandSyntaxException {
        MinecraftServer server = helper.getLevel().getServer();
        int dollars = server.getCommands().getDispatcher().execute("erranttrade money",
                server.createCommandSourceStack().withSuppressedOutput());
        assertCount(helper, "the command's result, in dollars", Math.min(Integer.MAX_VALUE, MoneySupply.measure(server).total() / 100), dollars);
        helper.succeed();
    }

    static long today(GameTestHelper helper) {
        return helper.getLevel().getDayTime() / 24000L;
    }

    private static final int GOLD_MARK = 1, COPPER_BIT = 0;

    /** Gives the villager a meeting point of its own, so each test's village is its own. */
    static GlobalPos joinVillage(GameTestHelper helper, Villager villager) {
        GlobalPos village = GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(new BlockPos(1, 1, 1)));
        villager.getBrain().setMemory(MemoryModuleType.MEETING_POINT, village);
        return village;
    }

    static long treasury(GameTestHelper helper, GlobalPos village) {
        return Treasuries.get(helper.getLevel().getServer()).balance(village);
    }

    static Villager banker(GameTestHelper helper) {
        Villager villager = VillagerInteractionGameTests.farmer(helper);
        villager.setVillagerData(villager.getVillagerData().setProfession(TradeVillagers.BANKER_PROFESSION.value()));
        return villager;
    }

    static long playerMoney(ServerPlayer player) {
        return PlayerMoney.loose(player).totalValue() + PlayerMoney.wallet(player).totalValue();
    }

    /** All the money the player and the villager hold: coins, wallet and till. */
    static long total(ServerPlayer player, Villager villager) {
        return playerMoney(player) + VillagerBalance.get(villager);
    }
}
