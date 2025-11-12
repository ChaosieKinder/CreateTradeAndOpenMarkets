package net.errantchaosie.tradeandopenmarkets.screen.shop;

import com.mojang.logging.LogUtils;
import net.errantchaosie.tradeandopenmarkets.shop.ShopTabManager;
import net.errantchaosie.tradeandopenmarkets.shop.tab.ShopTab;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.List;

import static net.errantchaosie.tradeandopenmarkets.util.InventoryHelper.*;

/**
 * Lightweight client-side menu wrapper that exposes the merchant information
 * required by {@link ShopScreen} without relying on the vanilla merchant UI
 * implementation.
 */
public class ShopMenu extends AbstractContainerMenu {
    // THIS YOU HAVE TO DEFINE!
    private static final int TE_INVENTORY_SLOT_COUNT = 0;  // must be the number of slots you have!
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Field MERCHANT_FIELD = findMerchantField();

    private final MerchantMenu merchantMenu;
    private final Inventory playerInventory;
    private final Level level;
    private final Component title;

    private MerchantOffers offers = new MerchantOffers();
    @Nullable
    private Merchant trader;
    private List<ShopTab> tabs = List.of();
    private int selectedTabIndex;

    public ShopMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(MenuType.MERCHANT, containerId);
        this.merchantMenu = null;
        this.playerInventory = inventory;
        this.level = playerInventory.player.level();
        this.title = Component.literal("ERROR OPENING SHOPMENU");
        addPlayerSlots();
        refresh();
    }

    public ShopMenu(int containerId, MerchantMenu merchantMenu, Inventory playerInventory, Component title) {
        super(MenuType.MERCHANT, containerId);
        this.merchantMenu = merchantMenu;
        this.playerInventory = playerInventory;
        this.level = playerInventory.player.level();
        this.title = title;
        addPlayerSlots();
        refresh();
    }

    public void addPlayerSlots() {
        // Player inventory + hotbar
        int invY = 93;
        int invX = 11;
        for (int r = 0; r < 3; ++r)
            for (int c = 0; c < 9; ++c)
                addSlot(new Slot(this.playerInventory, c + r * 9 + 9,
                        invX + c * 18, invY + r * 18));
        for (int c = 0; c < 9; ++c)
            addSlot(new Slot(this.playerInventory, c, invX + c * 20 - 7,
                    invY + 58));
    }

    public void refresh() {
        if (this.merchantMenu != null) {
            this.offers = this.merchantMenu.getOffers();
        }
        this.trader = extractTrader();
        if (this.level != null) {
            this.tabs = ShopTabManager.INSTANCE.resolveTabs(this.trader, this.offers, this.level);
        } else {
            this.tabs = List.of();
        }
        setSelectedTabIndex(this.selectedTabIndex);
    }

    public MerchantMenu getMerchantMenu() {
        return this.merchantMenu;
    }

    public Inventory getPlayerInventory() {
        return this.playerInventory;
    }

    public Component getTitle() {
        return this.title;
    }

    public MerchantOffers getOffers() {
        return this.offers;
    }

    public List<ShopTab> getTabs() {
        return this.tabs;
    }

    public int getSelectedTabIndex() {
        return this.selectedTabIndex;
    }

    public void setSelectedTabIndex(int selectedTabIndex) {
        if (this.tabs.isEmpty()) {
            this.selectedTabIndex = 0;
            return;
        }
        this.selectedTabIndex = Mth.clamp(selectedTabIndex, 0, this.tabs.size() - 1);
    }

    @Nullable
    public ShopTab getSelectedTab() {
        if (this.tabs.isEmpty()) {
            return null;
        }
        int index = Mth.clamp(this.selectedTabIndex, 0, this.tabs.size() - 1);
        return this.tabs.get(index);
    }

    @Nullable
    public Merchant getTrader() {
        return this.trader;
    }

    private Merchant extractTrader() {
        if (MERCHANT_FIELD == null) {
            return null;
        }

        try {
            return (Merchant) MERCHANT_FIELD.get(this.merchantMenu);
        } catch (IllegalAccessException e) {
            LOGGER.warn("Unable to access MerchantMenu trader field", e);
            return null;
        }
    }

    @Nullable
    private static Field findMerchantField() {
        try {
            Field field = ObfuscationReflectionHelper.findField(MerchantMenu.class, "trader");
            field.setAccessible(true);
            return field;
        } catch (RuntimeException e) {
            LOGGER.warn("Unable to locate MerchantMenu trader field; villager context will be unavailable", e);
            return null;
        }
    }


    @Override
    public ItemStack quickMoveStack(Player playerIn, int pIndex) {
        Slot sourceSlot = slots.get(pIndex);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;  //EMPTY_ITEM
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // Check if the slot clicked is one of the vanilla container slots
        if (pIndex < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // This is a vanilla container slot so merge the stack into the tile inventory
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX
                    + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;  // EMPTY_ITEM
            }
        } else if (pIndex < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // This is a TE slot so merge the stack into the players inventory
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            System.out.println("Invalid slotIndex:" + pIndex);
            return ItemStack.EMPTY;
        }
        // If stack size == 0 (the entire stack was moved) set slot contents to null
        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.merchantMenu != null && this.merchantMenu.stillValid(player);
    }
}