package net.errantchaosie.tradeandopenmarkets.screen.wallet;

import net.errantchaosie.tradeandopenmarkets.item.wallet.WalletInventory;
import net.errantchaosie.tradeandopenmarkets.screen.TAOMMenus;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;

import java.util.ArrayList;
import java.util.List;

public class WalletMenu extends AbstractContainerMenu {
    private final InteractionHand hand;
    private final ItemStack walletStack;
    private final SimpleContainer walletInv; // working inventory

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
}