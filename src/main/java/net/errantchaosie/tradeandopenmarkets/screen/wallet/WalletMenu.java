package net.errantchaosie.tradeandopenmarkets.screen.wallet;

import com.mojang.logging.LogUtils;
import net.errantchaosie.tradeandopenmarkets.TAOMTags;
import net.errantchaosie.tradeandopenmarkets.component.CurrencyData;
import net.errantchaosie.tradeandopenmarkets.component.WalletBalance;
import net.errantchaosie.tradeandopenmarkets.data.CoinDenomination;
import net.errantchaosie.tradeandopenmarkets.item.CoinItem;
import net.errantchaosie.tradeandopenmarkets.item.CoinStackItem;
import net.errantchaosie.tradeandopenmarkets.item.wallet.WalletInventory;
import net.errantchaosie.tradeandopenmarkets.screen.TAOMMenus;
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
    // Note: BTN_WITHDRAW_* has an additional option at +1000 for detecting shift state
    public static final int BTN_WITHDRAW_COPPER_BIT      = 10;
    public static final int BTN_WITHDRAW_COPPER_SHARD    = 11;
    public static final int BTN_WITHDRAW_SILVER_BIT      = 12;
    public static final int BTN_WITHDRAW_SILVER_CROWN    = 13;
    public static final int BTN_WITHDRAW_GOLD_MARK       = 14;
    public static final int BTN_WITHDRAW_GOLD_REGAL      = 15;
    public static final int BTN_WITHDRAW_EMERALD_SUN     = 16;
    public static final int BTN_WITHDRAW_EMERALD_THRONE  = 17;
    public static final int BTN_DEPOSIT_ALL = 50; // <— new button id
    private final Inventory playerInv;

    public static WalletMenu fromNetwork(int id, Inventory inv, FriendlyByteBuf buf) {
        InteractionHand hand = buf.readEnum(InteractionHand.class);
        return new WalletMenu(id, inv, hand);
    }

    public static final class Provider implements MenuProvider {
        private final InteractionHand hand;
        public Provider(InteractionHand hand) { this.hand = hand; }
        @Override public Component getDisplayName() { return Component.translatable("screen.tradeandopenmarkets.wallet_name"); }
        @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new WalletMenu(id, inv, hand); }
    }
    // CLIENT ctor: created from the buffer using the registration factory above
    public WalletMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, buf.readEnum(InteractionHand.class));
    }

    public WalletMenu(int id, Inventory playerInv, InteractionHand hand) {
        super(TAOMMenus.WALLET_MENU.get(), id);
        this.hand = hand;
        this.playerInv = playerInv;
        this.walletStack = playerInv.player.getItemInHand(hand);

        WalletInventory.ensure(walletStack);

        // Build a working SimpleContainer and prefill from the stack component
        this.walletInv = new SimpleContainer(WalletInventory.SIZE) {
            @Override public void setChanged() { super.setChanged(); } // hook if you want live syncing
            @Override public boolean stillValid(Player p) { return true; }
        };
        List<ItemStack> items = WalletInventory.read(walletStack);
        for (int i = 0; i < WalletInventory.SIZE; i++) walletInv.setItem(i, items.get(i));

        // Slots: 3x3 wallet grid (0..8)
        int x0 = 38, x1 = 65, x2 = 92,
            y0 = 50, y1 = 44, y2 = 40, idx = 0;
        for (int r = 0; r < 3; r++) {
            addSlot(new Slot(walletInv, idx++, x0, y0 + r * 33) {
                @Override public boolean mayPlace(ItemStack stack) {
                    // prevent putting the **same** wallet into itself
                    return stack.getItem() != walletStack.getItem();
                }
            });
        }
        for (int r = 0; r < 3; r++) {
            addSlot(new Slot(walletInv, idx++, x1, y1 + r * 34) {
                @Override public boolean mayPlace(ItemStack stack) {
                    // prevent putting the **same** wallet into itself
                    return stack.getItem() != walletStack.getItem();
                }
            });
        }
        for (int r = 0; r < 3; r++) {
            addSlot(new Slot(walletInv, idx++, x2, y2 + r * 32) {
                @Override public boolean mayPlace(ItemStack stack) {
                    // prevent putting the **same** wallet into itself
                    return stack.getItem() != walletStack.getItem();
                }
            });
        }

        // Player inventory + hotbar
        int invY = 163;
        for (int r = 0; r < 3; ++r)
            for (int c = 0; c < 9; ++c)
                addSlot(new Slot(playerInv, c + r * 9 + 9, 47 + c * 18, invY + r * 18));
        for (int c = 0; c < 9; ++c)
            addSlot(new Slot(playerInv, c, 47 + c * 18, invY + 58));
    }


    @Override
    public boolean stillValid(Player player) {
        // Keep open only while holding the same stack in that hand
        return !walletStack.isEmpty() && player.getItemInHand(hand) == walletStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && !walletStack.isEmpty() && player.getItemInHand(hand) == walletStack) {
            // Serialize SimpleContainer → List → component
            List<ItemStack> out = new ArrayList<>(WalletInventory.SIZE);
            for (int i = 0; i < WalletInventory.SIZE; i++) out.add(walletInv.getItem(i).copy());
            WalletInventory.write(walletStack, out);
        }
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
        boolean isShift = false;
        if(id > 1000) {
            id -= 1000;
            isShift = true;
        }
        LOG.info("Click Recieved: id={}, shift={}", id, isShift);
        return switch (id) {
            case BTN_WITHDRAW_COPPER_BIT     -> withdrawCoinsFromWallet(sp, CoinDenomination.COPPER_BIT, isShift);
            case BTN_WITHDRAW_COPPER_SHARD   -> withdrawCoinsFromWallet(sp, CoinDenomination.COPPER_SHARD, isShift);
            case BTN_WITHDRAW_SILVER_BIT     -> withdrawCoinsFromWallet(sp, CoinDenomination.SILVER_BIT, isShift);
            case BTN_WITHDRAW_SILVER_CROWN   -> withdrawCoinsFromWallet(sp, CoinDenomination.SILVER_CROWN, isShift);
            case BTN_WITHDRAW_GOLD_MARK      -> withdrawCoinsFromWallet(sp, CoinDenomination.GOLD_MARK, isShift);
            case BTN_WITHDRAW_GOLD_REGAL     -> withdrawCoinsFromWallet(sp, CoinDenomination.GOLD_REGAL, isShift);
            case BTN_WITHDRAW_EMERALD_SUN    -> withdrawCoinsFromWallet(sp, CoinDenomination.EMERALD_SUN, isShift);
            case BTN_WITHDRAW_EMERALD_THRONE -> withdrawCoinsFromWallet(sp, CoinDenomination.EMERALD_THRONE, isShift);
            case BTN_DEPOSIT_ALL -> {
                depositAllCoinsFromPlayer(sp);
                yield true;
            }
            default -> false;
        };
    }
    private boolean withdrawCoinsFromWallet(ServerPlayer sp, CoinDenomination denomonation, boolean isShift) {
        var result = WalletBalance.remove(walletStack, denomonation, isShift ? 64 : 1);
        if (result.delta() == 0) return false; // sanity check
        WalletBalance.set(walletStack, result.updated());
        sp.setItemInHand(hand, walletStack);   // re-set same ref to mark the slot dirty
        playerInv.setChanged();                // mark inventory dirty
        // give the coin item
        var coin = CoinItem.createCurrencyItem(result.denomination(), result.delta());
        if (result.delta() != 0) {
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
        CurrencyData cur = WalletBalance.get(walletStack);
        boolean changed = false;

        // 1) sweep carried (cursor) stack
        ItemStack carried = getCarried();
        if (!carried.isEmpty() && carried.getItem() instanceof CoinItem coin) {
            cur = cur.add(coin.denom(), carried.getCount()).updated();
            setCarried(ItemStack.EMPTY);
            changed = true;
        }

        // 2) sweep main inventory (includes hotbar)
        for (int i = 0; i < playerInv.items.size(); i++) {
            ItemStack s = playerInv.items.get(i);
            if (s.isEmpty()) continue;
            if (s.is(TAOMTags.Items.WALLET_ITEMS)) {
                if(s.getItem() instanceof CoinItem coin) {
                    cur = cur.add(coin.denom(), s.getCount()).updated();
                    playerInv.items.set(i, ItemStack.EMPTY);
                } else {
                    quickMoveStack(playerInv.player, i);
                }
                changed = true;
            }
        }

        // 3) sweep offhand
        for (int i = 0; i < playerInv.offhand.size(); i++) {
            ItemStack s = playerInv.offhand.get(i);
            if (s.isEmpty()) continue;
            if (s.is(TAOMTags.Items.WALLET_ITEMS)) {
                if(s.getItem() instanceof CoinItem coin) {
                    cur = cur.add(coin.denom(), s.getCount()).updated();
                    playerInv.offhand.set(i, ItemStack.EMPTY);
                } else {
                    quickMoveStack(playerInv.player, i);
                }
                changed = true;
            }
        }

        if (changed) {
            WalletBalance.set(walletStack, cur);
            sp.setItemInHand(hand, walletStack);   // re-set same ref to mark the slot dirty
            playerInv.setChanged();
            broadcastChanges();
        }
    }
    /** Sweep the 3×3 wallet grid; convert any coin stacks into CurrencyData, consuming the item. */
    private void autoDepositFromSlots() {
        // Server-side only
        if (!(this.getCarried() != null && this.slots != null)) { /* nothing */ }
        CurrencyData cur = WalletBalance.get(walletStack);
        boolean changed = false;

        for (int i = 0; i < WalletInventory.SIZE; i++) {
            ItemStack s = walletInv.getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof CoinItem coin) {
                int count = s.getCount();
                if (count <= 0) continue;

                // add to totals and clear the slot
                cur = cur.add(coin.denom(), count).updated();
                walletInv.setItem(i, ItemStack.EMPTY);
                changed = true;
            }
            if (s.getItem() instanceof CoinStackItem coinStack) {
                var data = CurrencyData.getFrom(s);
                cur = cur.add(data);
                walletInv.setItem(i, ItemStack.EMPTY);
                changed = true;
            }
        }

        if (changed) {
            WalletBalance.set(walletStack, cur);
            if (!( playerInv.player instanceof ServerPlayer sp))
            {
                playerInv.player.setItemInHand(hand, walletStack);   // re-set same ref to mark the slot dirty
            }
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