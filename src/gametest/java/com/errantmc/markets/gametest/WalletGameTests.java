package com.errantmc.markets.gametest;

import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.domain.Purse;
import com.errantmc.markets.component.Purses;
import com.errantmc.markets.domain.CoinDenomination;
import com.errantmc.markets.item.TradeItems;
import com.errantmc.markets.item.wallet.WalletInventory;
import com.errantmc.markets.screen.wallet.WalletMenu;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

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
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TradeItems.LEATHER_WALLET.get()));
        player.getInventory().setItem(2, coinStackOf(Purse.of(CoinDenomination.COPPER_BIT, 5)));
        player.setItemInHand(InteractionHand.OFF_HAND,
                coinStackOf(Purse.of(CoinDenomination.GOLD_MARK, 2)));
        player.getInventory().setItem(20, new ItemStack(TradeItems.COIN_SILVER_BIT.get(), 6));
        WalletMenu menu = new WalletMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);
        menu.getSlot(0).set(new ItemStack(TradeItems.ID_CARD.get()));
        menu.getSlot(2).set(new ItemStack(TradeItems.ID_CARD.get()));

        menu.clickMenuButton(player, WalletMenu.BTN_DEPOSIT_ALL);

        assertTrue(helper, "wallet slot 0 to keep its card", menu.getSlot(0).getItem().is(TradeItems.ID_CARD.get()));
        assertTrue(helper, "wallet slot 2 to keep its card", menu.getSlot(2).getItem().is(TradeItems.ID_CARD.get()));
        assertTrue(helper, "the hotbar coin stack to be taken", player.getInventory().getItem(2).isEmpty());
        assertTrue(helper, "the offhand coin stack to be taken", player.getOffhandItem().isEmpty());
        Purse balance = Purses.get(menu.getWalletStack());
        assertCount(helper, "copper bits in the wallet", 5, balance.count(CoinDenomination.COPPER_BIT));
        assertCount(helper, "gold marks in the wallet", 2, balance.count(CoinDenomination.GOLD_MARK));
        assertCount(helper, "silver bits in the wallet", 6, balance.count(CoinDenomination.SILVER_BIT));
        assertCount(helper, "coins left in the inventory", 0, coinsIn(player));
        helper.succeed();
    }

    /** Picking up coins with a wallet anywhere in the inventory puts them in the wallet (BACKLOG #9). */
    @GameTest(template = ARENA)
    public static void pickedUpCoinsGoIntoTheWallet(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack wallet = new ItemStack(TradeItems.LEATHER_WALLET.get());
        player.getInventory().setItem(14, wallet);
        ItemEntity coins = dropAt(helper, new ItemStack(TradeItems.COIN_SILVER_CROWN.get(), 5), true);

        coins.playerTouch(player);

        assertCount(helper, "silver crowns in the wallet", 5,
                Purses.get(wallet).count(CoinDenomination.SILVER_CROWN));
        assertCount(helper, "coins in the inventory", 0, coinsIn(player));
        assertTrue(helper, "the coins on the ground to be gone", coins.isRemoved());
        helper.succeed();
    }

    /** Freshly dropped coins keep vanilla's pickup delay; the wallet must not bypass it. */
    @GameTest(template = ARENA)
    public static void coinsStillInTheirPickupDelayAreLeftAlone(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack wallet = new ItemStack(TradeItems.LEATHER_WALLET.get());
        player.getInventory().setItem(14, wallet);
        ItemEntity coins = dropAt(helper, new ItemStack(TradeItems.COIN_SILVER_CROWN.get(), 5), false);

        coins.playerTouch(player);

        assertCount(helper, "silver crowns in the wallet", 0,
                Purses.get(wallet).count(CoinDenomination.SILVER_CROWN));
        assertTrue(helper, "the coins to stay on the ground", !coins.isRemoved());
        helper.succeed();
    }

    /** BACKLOG #3: a wallet that cannot hold them all takes what fits; vanilla picks up the rest. */
    @GameTest(template = ARENA)
    public static void pickupIntoANearlyFullWalletPutsTheRestInTheInventory(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack wallet = new ItemStack(TradeItems.LEATHER_WALLET.get());
        Purses.set(wallet, Purse.of(CoinDenomination.COPPER_BIT, Integer.MAX_VALUE - 1));
        player.getInventory().setItem(14, wallet);
        ItemEntity coins = dropAt(helper, new ItemStack(TradeItems.COIN_COPPER_BIT.get(), 5), true);

        coins.playerTouch(player);

        assertCount(helper, "copper bits in the wallet", Integer.MAX_VALUE,
                Purses.get(wallet).count(CoinDenomination.COPPER_BIT));
        assertCount(helper, "coins in the inventory", 4, coinsIn(player));
        helper.succeed();
    }

    // ---------- the wallet screen ----------

    @GameTest(template = ARENA)
    public static void withdrawGivesOneCoin(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack wallet = walletHolding(Purse.of(CoinDenomination.GOLD_REGAL, 3));
        player.setItemInHand(InteractionHand.MAIN_HAND, wallet);
        WalletMenu menu = new WalletMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);

        menu.clickMenuButton(player, WalletMenu.withdrawButton(CoinDenomination.GOLD_REGAL));

        assertCount(helper, "gold regals in the wallet", 2, Purses.get(wallet).count(CoinDenomination.GOLD_REGAL));
        assertCount(helper, "coins in the inventory", 1, coinsIn(player));
        helper.succeed();
    }

    /** The screen sends button id + 1000 when shift is held. */
    @GameTest(template = ARENA)
    public static void shiftWithdrawGivesAStack(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack wallet = walletHolding(Purse.of(CoinDenomination.COPPER_BIT, 100));
        player.setItemInHand(InteractionHand.MAIN_HAND, wallet);
        WalletMenu menu = new WalletMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);

        menu.clickMenuButton(player, 1000 + WalletMenu.withdrawButton(CoinDenomination.COPPER_BIT));

        assertCount(helper, "copper bits in the wallet", 36, Purses.get(wallet).count(CoinDenomination.COPPER_BIT));
        assertCount(helper, "coins in the inventory", 64, coinsIn(player));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void withdrawingFromAnEmptyDenominationDoesNothing(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack wallet = walletHolding(Purse.of(CoinDenomination.COPPER_BIT, 5));
        player.setItemInHand(InteractionHand.MAIN_HAND, wallet);
        WalletMenu menu = new WalletMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);

        boolean handled = menu.clickMenuButton(player, WalletMenu.withdrawButton(CoinDenomination.EMERALD_THRONE));

        assertTrue(helper, "the click to be refused", !handled);
        assertCount(helper, "coins in the inventory", 0, coinsIn(player));
        helper.succeed();
    }

    /** Nothing is lost when the inventory is full: the coins drop at the player's feet. */
    @GameTest(template = ARENA)
    public static void withdrawingWithAFullInventoryDropsTheCoins(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack wallet = walletHolding(Purse.of(CoinDenomination.SILVER_CROWN, 4));
        player.setItemInHand(InteractionHand.MAIN_HAND, wallet);
        for (int i = 1; i < player.getInventory().items.size(); i++) {
            player.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
        }
        WalletMenu menu = new WalletMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);

        menu.clickMenuButton(player, WalletMenu.withdrawButton(CoinDenomination.SILVER_CROWN));

        assertCount(helper, "silver crowns in the wallet", 3, Purses.get(wallet).count(CoinDenomination.SILVER_CROWN));
        var dropped = player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(3),
                e -> e.getItem().is(TradeItems.COIN_SILVER_CROWN.get()));
        assertCount(helper, "silver crowns dropped", 1, dropped.stream().mapToInt(e -> e.getItem().getCount()).sum());
        helper.succeed();
    }

    /** Coins and coin stacks put in the 3×3 grid turn into balance on the next sync. */
    @GameTest(template = ARENA)
    public static void coinsPutInTheGridBecomeBalance(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack wallet = new ItemStack(TradeItems.LEATHER_WALLET.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wallet);
        WalletMenu menu = new WalletMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);
        menu.getSlot(4).set(new ItemStack(TradeItems.COIN_COPPER_SHARD.get(), 10));
        menu.getSlot(7).set(coinStackOf(Purse.of(CoinDenomination.GOLD_MARK, 3)));

        menu.broadcastChanges();

        Purse balance = Purses.get(wallet);
        assertCount(helper, "copper shards in the wallet", 10, balance.count(CoinDenomination.COPPER_SHARD));
        assertCount(helper, "gold marks in the wallet", 3, balance.count(CoinDenomination.GOLD_MARK));
        assertTrue(helper, "grid slot 4 to be empty", menu.getSlot(4).getItem().isEmpty());
        assertTrue(helper, "grid slot 7 to be empty", menu.getSlot(7).getItem().isEmpty());
        helper.succeed();
    }

    /** The grid lives in a working container while the screen is open and is written back on close. */
    @GameTest(template = ARENA)
    public static void closingTheWalletKeepsWhatIsInItsGrid(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack wallet = new ItemStack(TradeItems.LEATHER_WALLET.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wallet);
        WalletMenu menu = new WalletMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);
        menu.getSlot(1).set(new ItemStack(TradeItems.ID_CARD.get()));

        menu.removed(player);

        assertTrue(helper, "the ID card to be saved in slot 1",
                WalletInventory.read(wallet).get(1).is(TradeItems.ID_CARD.get()));
        WalletMenu reopened = new WalletMenu(1, player.getInventory(), InteractionHand.MAIN_HAND);
        assertTrue(helper, "the ID card to be there on reopening",
                reopened.getSlot(1).getItem().is(TradeItems.ID_CARD.get()));
        helper.succeed();
    }

    /**
     * BACKLOG #16: the open wallet sits in one of the menu's own hotbar slots. Taking something
     * out of the grid and then throwing the wallet used to leave the thrown wallet holding it too.
     * The slot is now locked, and the grid is written through as it changes.
     */
    @GameTest(template = ARENA)
    public static void throwingTheOpenWalletDoesNotDuplicateItsGrid(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack wallet = new ItemStack(TradeItems.LEATHER_WALLET.get());
        List<ItemStack> grid = new ArrayList<>(WalletInventory.read(wallet));
        grid.set(1, new ItemStack(TradeItems.ID_CARD.get()));
        WalletInventory.write(wallet, grid);
        player.setItemInHand(InteractionHand.MAIN_HAND, wallet); // hotbar 0
        WalletMenu menu = new WalletMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);
        int walletSlot = WalletInventory.SIZE + 27; // grid, then main inventory, then hotbar 0

        menu.clicked(1, 0, ClickType.QUICK_MOVE, player);        // card: grid -> inventory
        menu.clicked(walletSlot, 0, ClickType.THROW, player);   // throw the open wallet
        menu.removed(player);

        int cards = 0;
        for (ItemStack s : player.getInventory().items) if (s.is(TradeItems.ID_CARD.get())) cards++;
        for (ItemEntity e : player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4))) {
            for (ItemStack s : WalletInventory.read(e.getItem())) if (s.is(TradeItems.ID_CARD.get())) cards++;
        }
        assertCount(helper, "ID cards in existence", 1, cards);
        assertTrue(helper, "the wallet to stay in hand", player.getMainHandItem() == wallet);
        helper.succeed();
    }

    /** A number key swaps straight from the inventory; it must not move the open wallet either. */
    @GameTest(template = ARENA)
    public static void aNumberKeyCannotSwapTheOpenWalletAway(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack wallet = new ItemStack(TradeItems.LEATHER_WALLET.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wallet); // hotbar 0
        WalletMenu menu = new WalletMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);

        menu.clicked(WalletInventory.SIZE, 0, ClickType.SWAP, player); // empty inventory slot <-> hotbar 0

        assertTrue(helper, "the wallet to stay in hand", player.getMainHandItem() == wallet);
        helper.succeed();
    }

    /** The grid reaches the wallet as it changes, not only when the screen closes. */
    @GameTest(template = ARENA)
    public static void theGridIsSavedAsItChanges(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack wallet = new ItemStack(TradeItems.LEATHER_WALLET.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wallet);
        WalletMenu menu = new WalletMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);

        menu.getSlot(3).set(new ItemStack(TradeItems.ID_CARD.get()));

        assertTrue(helper, "the wallet to hold the card before the screen closes",
                WalletInventory.read(wallet).get(3).is(TradeItems.ID_CARD.get()));
        helper.succeed();
    }

    /** BACKLOG #19: only what belongs in a real wallet fits; containers above all do not. */
    @GameTest(template = ARENA)
    public static void theGridTakesOnlyWalletThings(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TradeItems.LEATHER_WALLET.get()));
        WalletMenu menu = new WalletMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);
        var grid = menu.getSlot(0);

        assertTrue(helper, "a coin to fit", grid.mayPlace(new ItemStack(TradeItems.COIN_GOLD_MARK.get())));
        assertTrue(helper, "a coin stack to fit", grid.mayPlace(new ItemStack(TradeItems.COIN_STACK_ITEM.get())));
        assertTrue(helper, "an ID card to fit", grid.mayPlace(new ItemStack(TradeItems.ID_CARD.get())));
        assertTrue(helper, "a shulker box not to fit", !grid.mayPlace(new ItemStack(Items.SHULKER_BOX)));
        assertTrue(helper, "a bundle not to fit", !grid.mayPlace(new ItemStack(Items.BUNDLE)));
        assertTrue(helper, "dirt not to fit", !grid.mayPlace(new ItemStack(Items.DIRT)));
        helper.succeed();
    }

    /** Deposit-all takes money only; ID cards in the inventory stay where they are. */
    @GameTest(template = ARENA)
    public static void depositAllLeavesIdCardsInTheInventory(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TradeItems.LEATHER_WALLET.get()));
        player.getInventory().setItem(7, new ItemStack(TradeItems.ID_CARD.get()));
        WalletMenu menu = new WalletMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);

        menu.clickMenuButton(player, WalletMenu.BTN_DEPOSIT_ALL);

        assertTrue(helper, "the ID card to stay in slot 7", player.getInventory().getItem(7).is(TradeItems.ID_CARD.get()));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void aWalletCannotGoInsideAWallet(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TradeItems.LEATHER_WALLET.get()));
        WalletMenu menu = new WalletMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);

        for (int slot = 0; slot < WalletInventory.SIZE; slot++) {
            assertTrue(helper, "grid slot " + slot + " to refuse a wallet",
                    !menu.getSlot(slot).mayPlace(new ItemStack(TradeItems.LEATHER_WALLET.get())));
        }
        helper.succeed();
    }

    /** The balance is a data component, so it is a save format: it must survive a round trip. */
    @GameTest(template = ARENA)
    public static void aWalletKeepsItsBalanceThroughSaving(GameTestHelper helper) {
        Purse balance = Purse.of(CoinDenomination.EMERALD_SUN, 4)
                .add(CoinDenomination.COPPER_BIT, 9).updated();
        ItemStack wallet = walletHolding(balance);
        var registries = helper.getLevel().registryAccess();

        ItemStack loaded = ItemStack.parseOptional(registries, (CompoundTag) wallet.save(registries));

        if (!Purses.get(loaded).equals(balance)) {
            helper.fail("loaded " + Purses.get(loaded) + ", saved " + balance);
        }
        helper.succeed();
    }

    private static ItemStack walletHolding(Purse balance) {
        ItemStack wallet = new ItemStack(TradeItems.LEATHER_WALLET.get());
        Purses.set(wallet, balance);
        return wallet;
    }

    private static ItemEntity dropAt(GameTestHelper helper, ItemStack stack, boolean pickable) {
        Vec3 at = helper.absoluteVec(new Vec3(4.5, 2, 4.5));
        ItemEntity entity = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, stack);
        if (pickable) entity.setNoPickUpDelay();
        else entity.setDefaultPickUpDelay();
        helper.getLevel().addFreshEntity(entity);
        return entity;
    }

    /** BACKLOG #3: what does not fit under Integer.MAX_VALUE stays in the inventory. */
    @GameTest(template = ARENA)
    public static void depositAllIntoANearlyFullWalletLeavesTheRest(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack wallet = new ItemStack(TradeItems.LEATHER_WALLET.get());
        Purses.set(wallet, Purse.of(CoinDenomination.COPPER_BIT, Integer.MAX_VALUE - 1));
        player.setItemInHand(InteractionHand.MAIN_HAND, wallet);
        player.getInventory().setItem(5, new ItemStack(TradeItems.COIN_COPPER_BIT.get(), 5));
        WalletMenu menu = new WalletMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);

        menu.clickMenuButton(player, WalletMenu.BTN_DEPOSIT_ALL);

        assertCount(helper, "copper bits in the wallet", Integer.MAX_VALUE,
                Purses.get(menu.getWalletStack()).count(CoinDenomination.COPPER_BIT));
        assertCount(helper, "coins left in the inventory", 4, coinsIn(player));
        helper.succeed();
    }
}
