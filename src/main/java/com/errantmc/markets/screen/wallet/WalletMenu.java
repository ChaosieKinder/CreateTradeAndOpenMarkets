package com.errantmc.markets.screen.wallet;

import com.mojang.logging.LogUtils;
import com.errantmc.markets.TradeTags;
import com.errantmc.markets.domain.Purse;
import com.errantmc.markets.component.Purses;
import com.errantmc.markets.domain.CoinDenomination;
import com.errantmc.markets.item.CoinDeposits;
import com.errantmc.markets.item.CoinItem;
import com.errantmc.markets.item.CoinStackItem;
import com.errantmc.markets.item.wallet.WalletInventory;
import com.errantmc.markets.item.wallet.WalletItem;
import com.errantmc.markets.screen.TradeMenus;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class WalletMenu extends AbstractContainerMenu {
    public static final Logger LOG = LogUtils.getLogger();
    private final InteractionHand hand;
    private final ItemStack walletStack;
    private final SimpleContainer walletInv; // working inventory

    // Button IDs (unique and stable)
    /** Withdraw buttons are 10..17, one per denomination; the screen adds 1000 when shift is held. */
    private static final int BTN_WITHDRAW_FIRST = 10;
    private static final int SHIFT = 1000;

    public static int withdrawButton(CoinDenomination denomination) {
        return BTN_WITHDRAW_FIRST + CoinDenomination.coins().indexOf(denomination);
    }
    public static final int BTN_DEPOSIT_ALL = 50;
    private final Inventory playerInv;

    public static WalletMenu fromNetwork(int id, Inventory inv, FriendlyByteBuf buf) {
        InteractionHand hand = buf.readEnum(InteractionHand.class);
        return new WalletMenu(id, inv, hand);
    }

    public static final class Provider implements MenuProvider {
        private final InteractionHand hand;
        public Provider(InteractionHand hand) { this.hand = hand; }
        @Override public Component getDisplayName() { return Component.translatable("screen.erranttrade.wallet_name"); }
        @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new WalletMenu(id, inv, hand); }
    }
    // CLIENT ctor: created from the buffer using the registration factory above
    public WalletMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, buf.readEnum(InteractionHand.class));
    }

    public WalletMenu(int id, Inventory playerInv, InteractionHand hand) {
        super(TradeMenus.WALLET_MENU.get(), id);
        this.hand = hand;
        this.playerInv = playerInv;
        this.walletStack = playerInv.player.getItemInHand(hand);

        WalletInventory.ensure(walletStack);

        // A working copy of the grid, written through to the wallet on every change, so the wallet
        // never holds a stale grid however the screen ends (BACKLOG #16)
        this.walletInv = new SimpleContainer(WalletInventory.SIZE) {
            @Override public void setChanged() {
                super.setChanged();
                if (!playerInv.player.level().isClientSide) writeGrid();
            }
            @Override public boolean stillValid(Player p) { return true; }
        };
        List<ItemStack> items = WalletInventory.read(walletStack);
        for (int i = 0; i < WalletInventory.SIZE; i++) walletInv.setItem(i, items.get(i));

        // Slots: 3x3 wallet grid (0..8), in three slightly fanned columns
        int[] columnX = {38, 65, 92}, columnY = {50, 44, 40}, rowStep = {33, 34, 32};
        int idx = 0;
        for (int col = 0; col < 3; col++) {
            for (int r = 0; r < 3; r++) {
                addSlot(new Slot(walletInv, idx++, columnX[col], columnY[col] + r * rowStep[col]) {
                    /** Only what belongs in a real wallet: the wallet_items tag (BACKLOG #19) */
                    @Override public boolean mayPlace(ItemStack stack) {
                        return stack.is(TradeTags.Items.WALLET_ITEMS) && !(stack.getItem() instanceof WalletItem);
                    }
                });
            }
        }

        // Player inventory + hotbar. The open wallet's own slot is locked: moving the wallet
        // while its screen is open is how its grid was duplicated
        int lockedSlot = hand == InteractionHand.MAIN_HAND ? playerInv.selected : -1;
        int invY = 163;
        for (int r = 0; r < 3; ++r)
            for (int c = 0; c < 9; ++c)
                addSlot(new Slot(playerInv, c + r * 9 + 9, 47 + c * 18, invY + r * 18));
        for (int c = 0; c < 9; ++c) {
            addSlot(c == lockedSlot ? new LockedSlot(playerInv, c, 47 + c * 18, invY + 58)
                                    : new Slot(playerInv, c, 47 + c * 18, invY + 58));
        }
    }

    /** Holds the open wallet: it cannot be picked up, thrown, swapped or replaced. */
    private static final class LockedSlot extends Slot {
        LockedSlot(Inventory inv, int index, int x, int y) { super(inv, index, x, y); }
        @Override public boolean mayPickup(Player player) { return false; }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }

    /** Number keys and the offhand key swap straight from the inventory, past any slot's checks. */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (clickType == ClickType.SWAP) {
            boolean walletIsTheSwapTarget = hand == InteractionHand.MAIN_HAND
                    ? button == playerInv.selected
                    : button == Inventory.SLOT_OFFHAND;
            if (walletIsTheSwapTarget) return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    private void writeGrid() {
        if (walletStack.isEmpty()) return;
        List<ItemStack> out = new ArrayList<>(WalletInventory.SIZE);
        for (int i = 0; i < WalletInventory.SIZE; i++) out.add(walletInv.getItem(i).copy());
        WalletInventory.write(walletStack, out);
    }


    @Override
    public boolean stillValid(Player player) {
        // Keep open only while holding the same stack in that hand
        return !walletStack.isEmpty() && player.getItemInHand(hand) == walletStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) writeGrid();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) return empty;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        int walletStart = 0;
        int walletEnd   = walletStart + WalletInventory.SIZE;
        int invStart    = walletEnd;
        int invEnd      = invStart + 36;

        if (slotIndex < walletEnd) {
            // wallet → player
            if (!moveItemStackTo(stack, invStart, invEnd, true)) return empty;
        } else {
            // player → wallet
            if (!moveItemStackTo(stack, walletStart, walletEnd, false)) return empty;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer sp)) return false;
        boolean isShift = id > SHIFT;
        if (isShift) id -= SHIFT;
        if (id == BTN_DEPOSIT_ALL) {
            depositAllCoinsFromPlayer(sp);
            return true;
        }
        int coin = id - BTN_WITHDRAW_FIRST;
        var coins = CoinDenomination.coins();
        return coin >= 0 && coin < coins.size() && withdrawCoinsFromWallet(sp, coins.get(coin), isShift);
    }

    private boolean withdrawCoinsFromWallet(ServerPlayer sp, CoinDenomination denomination, boolean isShift) {
        var result = Purses.remove(walletStack, denomination, isShift ? 64 : 1);
        if (result.moved() == 0) return false; // sanity check
        Purses.set(walletStack, result.updated());
        sp.setItemInHand(hand, walletStack);   // re-set same ref to mark the slot dirty
        playerInv.setChanged();                // mark inventory dirty
        // give the coin item
        var coin = CoinItem.createCurrencyItem(result.denomination(), result.moved());
        if (result.moved() != 0) {
            if (!sp.getInventory().add(coin)) {
                sp.drop(coin, false);
            }
        }
        slotsChanged(this.walletInv);
        sp.setItemInHand(hand, walletStack);
        broadcastChanges();
        return true;
    }

    // WalletMenu: expose which hand was used
    public InteractionHand getHand() { return hand; }

    private void depositAllCoinsFromPlayer(ServerPlayer sp) {
        Purse before = Purses.get(walletStack);
        Purse cur = CoinDeposits.absorb(before, getCarried());
        // items is the main inventory and hotbar; the open wallet is never a coin, so it is skipped
        for (ItemStack s : playerInv.items) {
            if (s.is(TradeTags.Items.CURRENCY_ITEMS)) cur = CoinDeposits.absorb(cur, s);
        }
        for (ItemStack s : playerInv.offhand) {
            if (s.is(TradeTags.Items.CURRENCY_ITEMS)) cur = CoinDeposits.absorb(cur, s);
        }

        if (!cur.equals(before)) {
            Purses.set(walletStack, cur);
            sp.setItemInHand(hand, walletStack);   // re-set same ref to mark the slot dirty
            playerInv.setChanged();
            broadcastChanges();
        }
    }

    /** Sweep the 3×3 wallet grid, turning coins and coin stacks into balance. */
    private void autoDepositFromSlots() {
        Purse before = Purses.get(walletStack);
        Purse cur = before;
        for (int i = 0; i < WalletInventory.SIZE; i++) {
            ItemStack s = walletInv.getItem(i);
            if (s.isEmpty()) continue;
            cur = CoinDeposits.absorb(cur, s);
            if (s.isEmpty()) walletInv.setItem(i, ItemStack.EMPTY);
        }

        if (!cur.equals(before)) {
            Purses.set(walletStack, cur);
            walletInv.setChanged();
            playerInv.setChanged();
            broadcastChanges();
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        // Only run server-side
        if (this.getSlot(0) == null || (this.playerInv.player == null) || this.playerInv.player.level().isClientSide) return;
        autoDepositFromSlots();
    }

    // expose wallet stack to the screen for drawing:
    public ItemStack getWalletStack() { return walletStack; }
}