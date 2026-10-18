package com.errantmc.markets.gametest;

import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.component.Purses;
import com.errantmc.markets.domain.CoinDenomination;
import com.errantmc.markets.domain.Purse;
import com.errantmc.markets.item.PlayerMoney;
import com.errantmc.markets.item.TradeItems;
import com.errantmc.markets.npcinteractions.components.SimpleShopTab;
import com.errantmc.markets.npcinteractions.trading.ShopEntry;
import com.errantmc.markets.screen.villager.VillagerInteractionMenu;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.errantmc.markets.gametest.GameTests.*;

/**
 * Buying and selling through the interaction menu, on the farmer's shop
 * (data/erranttrade/npcinteractions/farmer.json): tab 0 sells 6 bread for 100 copper bits, tab 1
 * buys 20 wheat for 100.
 */
@GameTestHolder(ErrantTrade.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TradingGameTests {
    private TradingGameTests() {}

    private static final int BUY = 0, SELL = 1, BREAD = 0, WHEAT = 0;

    @GameTest(template = ARENA)
    public static void buyingPaysWithLooseCoins(GameTestHelper helper) {
        Villager farmer = VillagerInteractionGameTests.farmer(helper);
        ServerPlayer player = survivalPlayer(helper);
        player.getInventory().setItem(3, new ItemStack(TradeItems.COIN_SILVER_CROWN.get(), 1));
        VillagerInteractionMenu menu = open(helper, player, farmer);
        if (menu == null) return;
        int stockBefore = entry(menu, BUY, BREAD).stock();

        menu.clickMenuButton(player, VillagerInteractionMenu.tradeButton(BUY, BREAD));

        assertCount(helper, "bread", 6, count(player, Items.BREAD));
        assertCount(helper, "coins left", 0, PlayerMoney.loose(player).totalValue());
        assertCount(helper, "bread stock", stockBefore - 1, entry(menu, BUY, BREAD).stock());
        helper.succeed();
    }

    /** Loose coins go first, then the wallet; change goes into the wallet. */
    @GameTest(template = ARENA)
    public static void looseCoinsAreSpentBeforeTheWallet(GameTestHelper helper) {
        Villager farmer = VillagerInteractionGameTests.farmer(helper);
        ServerPlayer player = survivalPlayer(helper);
        player.getInventory().setItem(3, new ItemStack(TradeItems.COIN_COPPER_SHARD.get(), 4)); // 20 bits
        ItemStack wallet = new ItemStack(TradeItems.LEATHER_WALLET.get());
        Purses.set(wallet, Purse.of(CoinDenomination.SILVER_CROWN, 3));                          // 300 bits
        player.getInventory().setItem(8, wallet);
        VillagerInteractionMenu menu = open(helper, player, farmer);
        if (menu == null) return;

        menu.clickMenuButton(player, VillagerInteractionMenu.tradeButton(BUY, BREAD));

        assertCount(helper, "loose coins left", 0, PlayerMoney.loose(player).totalValue());
        assertCount(helper, "wallet balance (300 + 20 - 100, the 20 back as change)", 220, Purses.get(wallet).totalValue());
        assertCount(helper, "bread", 6, count(player, Items.BREAD));
        helper.succeed();
    }

    /** A refused trade changes nothing. */
    @GameTest(template = ARENA)
    public static void notEnoughMoneyBuysNothing(GameTestHelper helper) {
        Villager farmer = VillagerInteractionGameTests.farmer(helper);
        ServerPlayer player = survivalPlayer(helper);
        player.getInventory().setItem(3, new ItemStack(TradeItems.COIN_COPPER_BIT.get(), 99));
        VillagerInteractionMenu menu = open(helper, player, farmer);
        if (menu == null) return;
        int stockBefore = entry(menu, BUY, BREAD).stock();

        menu.clickMenuButton(player, VillagerInteractionMenu.tradeButton(BUY, BREAD));

        assertCount(helper, "bread", 0, count(player, Items.BREAD));
        assertCount(helper, "coins left", 99, PlayerMoney.loose(player).totalValue());
        assertCount(helper, "bread stock", stockBefore, entry(menu, BUY, BREAD).stock());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void anEntryOutOfStockCannotBeBought(GameTestHelper helper) {
        Villager farmer = VillagerInteractionGameTests.farmer(helper);
        ServerPlayer player = survivalPlayer(helper);
        player.getInventory().setItem(3, new ItemStack(TradeItems.COIN_GOLD_MARK.get(), 5));
        VillagerInteractionMenu menu = open(helper, player, farmer);
        if (menu == null) return;
        entry(menu, BUY, BREAD).setStock(0);

        menu.clickMenuButton(player, VillagerInteractionMenu.tradeButton(BUY, BREAD));

        assertCount(helper, "bread", 0, count(player, Items.BREAD));
        assertCount(helper, "coins left", 2500, PlayerMoney.loose(player).totalValue());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void sellingPaysIntoTheWallet(GameTestHelper helper) {
        Villager farmer = VillagerInteractionGameTests.farmer(helper);
        ServerPlayer player = survivalPlayer(helper);
        player.getInventory().setItem(3, new ItemStack(Items.WHEAT, 25));
        ItemStack wallet = new ItemStack(TradeItems.LEATHER_WALLET.get());
        player.getInventory().setItem(8, wallet);
        VillagerInteractionMenu menu = open(helper, player, farmer);
        if (menu == null) return;

        menu.clickMenuButton(player, VillagerInteractionMenu.tradeButton(SELL, WHEAT));

        assertCount(helper, "wheat left", 5, count(player, Items.WHEAT));
        assertCount(helper, "wallet balance", 100, Purses.get(wallet).totalValue());
        assertCount(helper, "loose coins", 0, PlayerMoney.loose(player).totalValue());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void sellingWithoutAWalletPaysInCoins(GameTestHelper helper) {
        Villager farmer = VillagerInteractionGameTests.farmer(helper);
        ServerPlayer player = survivalPlayer(helper);
        player.getInventory().setItem(3, new ItemStack(Items.WHEAT, 20));
        VillagerInteractionMenu menu = open(helper, player, farmer);
        if (menu == null) return;

        menu.clickMenuButton(player, VillagerInteractionMenu.tradeButton(SELL, WHEAT));

        assertCount(helper, "wheat left", 0, count(player, Items.WHEAT));
        assertCount(helper, "silver crowns", 1, count(player, TradeItems.COIN_SILVER_CROWN.get()));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void sellingWhatYouDoNotHaveIsRefused(GameTestHelper helper) {
        Villager farmer = VillagerInteractionGameTests.farmer(helper);
        ServerPlayer player = survivalPlayer(helper);
        player.getInventory().setItem(3, new ItemStack(Items.WHEAT, 19));
        VillagerInteractionMenu menu = open(helper, player, farmer);
        if (menu == null) return;

        menu.clickMenuButton(player, VillagerInteractionMenu.tradeButton(SELL, WHEAT));

        assertCount(helper, "wheat left", 19, count(player, Items.WHEAT));
        assertCount(helper, "coins", 0, PlayerMoney.loose(player).totalValue());
        helper.succeed();
    }

    /**
     * Villager experience scales with the trade's worth (author's decision): 100 copper bits at the
     * default 50 per point is 2 points, and the player gets the same in orbs, not vanilla's fixed
     * amount per trade.
     */
    @GameTest(template = ARENA)
    public static void aTradeGivesExperienceByItsWorth(GameTestHelper helper) {
        Villager farmer = VillagerInteractionGameTests.farmer(helper);
        ServerPlayer player = survivalPlayer(helper);
        player.getInventory().setItem(3, new ItemStack(TradeItems.COIN_SILVER_CROWN.get(), 1));
        VillagerInteractionMenu menu = open(helper, player, farmer);
        if (menu == null) return;
        int xpBefore = farmer.getVillagerXp();

        menu.clickMenuButton(player, VillagerInteractionMenu.tradeButton(BUY, BREAD));

        assertCount(helper, "villager experience gained", 2, farmer.getVillagerXp() - xpBefore);
        int orbs = helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, farmer.getBoundingBox().inflate(3))
                .stream().mapToInt(ExperienceOrb::getValue).sum();
        assertCount(helper, "player experience in orbs", 2, orbs);
        helper.succeed();
    }

    // ---------- helpers ----------

    static VillagerInteractionMenu open(GameTestHelper helper, ServerPlayer player, Villager villager) {
        player.interactOn(villager, InteractionHand.MAIN_HAND);
        if (player.containerMenu instanceof VillagerInteractionMenu menu) return menu;
        helper.fail("the interaction menu did not open");
        return null;
    }

    static ShopEntry entry(VillagerInteractionMenu menu, int tab, int entry) {
        return ((SimpleShopTab) menu.getTabs().get(tab)).entries().get(entry);
    }

    static int count(ServerPlayer player, Item item) {
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }
}
