package com.errantmc.markets.gametest;

import com.errantmc.markets.Config;
import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.domain.Payment;
import com.errantmc.markets.item.PlayerMoney;
import com.errantmc.markets.npcinteractions.components.InteractionTab;
import com.errantmc.markets.npcinteractions.components.SimpleShopTab;
import com.errantmc.markets.npcinteractions.tabs.buy.BuyShopTab;
import com.errantmc.markets.npcinteractions.tabs.sell.SellShopTab;
import com.errantmc.markets.npcinteractions.trading.ShopEntry;
import com.errantmc.markets.screen.villager.VillagerInteractionMenu;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

import static com.errantmc.markets.gametest.GameTests.*;

/**
 * Villagers without a shop definition trade their vanilla offers, converted. The toolsmith here is
 * given fixed offers so every path runs every time: {@link #COAL} the player sells, {@link #AXE}
 * the player buys, {@link #BOOK_AND_QUILL} costs emeralds and a book.
 */
@GameTestHolder(ErrantTrade.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VanillaTradingGameTests {
    private VanillaTradingGameTests() {}

    private static final int COAL = 0, AXE = 1, BOOK_AND_QUILL = 2;

    /** Every offer becomes one entry: price at the emerald rate, stock its remaining uses. */
    @GameTest(template = ARENA)
    public static void vanillaOffersAreConvertedAtTheEmeraldRate(GameTestHelper helper) {
        Villager toolsmith = toolsmith(helper);
        ServerPlayer player = survivalPlayer(helper);
        VillagerInteractionMenu menu = TradingGameTests.open(helper, player, toolsmith);
        if (menu == null) return;
        int rate = Config.EMERALD_VALUE.get();
        var offers = toolsmith.getOffers();
        assertTrue(helper, "the toolsmith to have offers", !offers.isEmpty());

        int linked = 0;
        for (InteractionTab tab : menu.getTabs()) {
            for (ShopEntry entry : ((SimpleShopTab) tab).entries()) {
                MerchantOffer offer = offers.get(entry.offerIndex());
                linked++;
                assertCount(helper, "stock of " + entry.item(), offer.getMaxUses() - offer.getUses(), entry.stock());
                if (tab instanceof SellShopTab) {
                    assertCount(helper, "pay for " + entry.item(), (long) offer.getResult().getCount() * rate, entry.price());
                } else {
                    assertCount(helper, "price of " + entry.item(), (long) emeralds(offer) * rate, entry.price());
                    assertTrue(helper, "the bought item to be the offer's result", ItemStack.matches(offer.getResult(), entry.item()));
                }
            }
        }
        assertCount(helper, "entries linked to offers", offers.size(), linked);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void sellingToAVanillaVillagerUsesItsOffer(GameTestHelper helper) {
        Villager toolsmith = toolsmith(helper);
        ServerPlayer player = survivalPlayer(helper);
        VillagerInteractionMenu menu = TradingGameTests.open(helper, player, toolsmith);
        if (menu == null) return;
        int tab = tabOf(menu, SellShopTab.class);
        if (tab < 0) { helper.fail("no such tab"); return; }
        ShopEntry entry = TradingGameTests.entry(menu, tab, 0);
        MerchantOffer offer = toolsmith.getOffers().get(entry.offerIndex());
        int usesBefore = offer.getUses();
        player.getInventory().setItem(3, entry.item().copy());
        if (!entry.extraCost().isEmpty()) player.getInventory().setItem(4, entry.extraCost().copy());

        menu.clickMenuButton(player, VillagerInteractionMenu.tradeButton(tab, 0));

        assertCount(helper, "offer uses", usesBefore + 1, offer.getUses());
        assertCount(helper, "coins received", entry.price(), PlayerMoney.loose(player).totalValue());
        assertCount(helper, "stock shown afterwards", offer.getMaxUses() - offer.getUses(),
                TradingGameTests.entry(menu, tab, 0).stock());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void buyingFromAVanillaVillagerUsesItsOffer(GameTestHelper helper) {
        Villager toolsmith = toolsmith(helper);
        ServerPlayer player = survivalPlayer(helper);
        VillagerInteractionMenu menu = TradingGameTests.open(helper, player, toolsmith);
        if (menu == null) return;
        int tab = tabOf(menu, BuyShopTab.class);
        if (tab < 0) { helper.fail("no such tab"); return; }
        ShopEntry entry = TradingGameTests.entry(menu, tab, 0);
        MerchantOffer offer = toolsmith.getOffers().get(entry.offerIndex());
        int usesBefore = offer.getUses();
        PlayerMoney.give(player, Payment.change(entry.price()));
        if (!entry.extraCost().isEmpty()) player.getInventory().setItem(30, entry.extraCost().copy());

        menu.clickMenuButton(player, VillagerInteractionMenu.tradeButton(tab, 0));

        assertCount(helper, "offer uses", usesBefore + 1, offer.getUses());
        assertCount(helper, "coins left", 0, PlayerMoney.loose(player).totalValue());
        assertCount(helper, "items bought", entry.item().getCount(), TradingGameTests.count(player, entry.item().getItem()));
        helper.succeed();
    }

    /** A used-up vanilla offer stays used up until the villager restocks at its workstation. */
    @GameTest(template = ARENA)
    public static void aUsedUpVanillaOfferIsOutOfStock(GameTestHelper helper) {
        Villager toolsmith = toolsmith(helper);
        for (MerchantOffer offer : toolsmith.getOffers()) {
            while (!offer.isOutOfStock()) offer.increaseUses();
        }
        ServerPlayer player = survivalPlayer(helper);
        VillagerInteractionMenu menu = TradingGameTests.open(helper, player, toolsmith);
        if (menu == null) return;
        int tab = tabOf(menu, BuyShopTab.class);
        if (tab < 0) { helper.fail("no such tab"); return; }
        ShopEntry entry = TradingGameTests.entry(menu, tab, 0);
        assertCount(helper, "stock shown", 0, entry.stock());
        PlayerMoney.give(player, Payment.change(entry.price()));
        if (!entry.extraCost().isEmpty()) player.getInventory().setItem(30, entry.extraCost().copy());

        menu.clickMenuButton(player, VillagerInteractionMenu.tradeButton(tab, 0));

        assertCount(helper, "coins kept", entry.price(), PlayerMoney.loose(player).totalValue());
        helper.succeed();
    }

    /** A second cost that is not emeralds becomes an item the player must also hand over. */
    @GameTest(template = ARENA)
    public static void anOffersSecondCostMustBeHandedOver(GameTestHelper helper) {
        Villager toolsmith = toolsmith(helper);
        ServerPlayer player = survivalPlayer(helper);
        VillagerInteractionMenu menu = TradingGameTests.open(helper, player, toolsmith);
        if (menu == null) return;
        int tab = tabOf(menu, BuyShopTab.class);
        int index = indexOfOffer(menu, tab, BOOK_AND_QUILL);
        ShopEntry entry = TradingGameTests.entry(menu, tab, index);
        assertTrue(helper, "the book to be the extra cost", entry.extraCost().is(Items.BOOK));
        assertCount(helper, "price (5 emeralds)", 5L * Config.EMERALD_VALUE.get(), entry.price());
        PlayerMoney.give(player, Payment.change(entry.price()));

        menu.clickMenuButton(player, VillagerInteractionMenu.tradeButton(tab, index));
        assertCount(helper, "book and quills without a book", 0, TradingGameTests.count(player, Items.WRITABLE_BOOK));

        player.getInventory().setItem(30, new ItemStack(Items.BOOK));
        menu.clickMenuButton(player, VillagerInteractionMenu.tradeButton(tab, index));
        assertCount(helper, "book and quills with a book", 1, TradingGameTests.count(player, Items.WRITABLE_BOOK));
        assertCount(helper, "books left", 0, TradingGameTests.count(player, Items.BOOK));
        assertCount(helper, "coins left", 0, PlayerMoney.loose(player).totalValue());
        helper.succeed();
    }

    private static Villager toolsmith(GameTestHelper helper) {
        Villager villager = helper.spawn(EntityType.VILLAGER, new Vec3(4.5, 1, 4.5));
        villager.setVillagerData(villager.getVillagerData().setProfession(VillagerProfession.TOOLSMITH));
        villager.setNoAi(true);
        MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(new ItemCost(Items.COAL, 15), new ItemStack(Items.EMERALD), 16, 2, 0.05f));
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(Items.STONE_AXE), 12, 1, 0.2f));
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 5), Optional.of(new ItemCost(Items.BOOK)),
                new ItemStack(Items.WRITABLE_BOOK), 12, 5, 0.05f));
        villager.setOffers(offers);
        return villager;
    }

    private static int indexOfOffer(VillagerInteractionMenu menu, int tab, int offerIndex) {
        var entries = ((SimpleShopTab) menu.getTabs().get(tab)).entries();
        for (int i = 0; i < entries.size(); i++) if (entries.get(i).offerIndex() == offerIndex) return i;
        return -1;
    }

    private static int tabOf(VillagerInteractionMenu menu, Class<? extends SimpleShopTab> type) {
        List<InteractionTab> tabs = menu.getTabs();
        for (int i = 0; i < tabs.size(); i++) {
            if (type.isInstance(tabs.get(i)) && !((SimpleShopTab) tabs.get(i)).entries().isEmpty()) return i;
        }
        return -1;
    }

    private static int emeralds(MerchantOffer offer) {
        int n = 0;
        if (offer.getCostA().is(Items.EMERALD)) n += offer.getCostA().getCount();
        if (offer.getCostB().is(Items.EMERALD)) n += offer.getCostB().getCount();
        return n;
    }
}
