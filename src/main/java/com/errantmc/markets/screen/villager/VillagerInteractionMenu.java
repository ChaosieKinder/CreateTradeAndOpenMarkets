package com.errantmc.markets.screen.villager;

import com.mojang.logging.LogUtils;
import com.errantmc.markets.npcinteractions.registry.InteractionTabManager;
import com.errantmc.markets.npcinteractions.components.InteractionTab;
import com.errantmc.markets.screen.TradeMenus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;

import static com.errantmc.markets.util.InventoryHelper.*;

/**
 * Lightweight client-side menu wrapper that exposes the merchant information
 * required by {@link VillagerInteractionScreen} without relying on the vanilla merchant UI
 * implementation.
 */
public class VillagerInteractionMenu extends AbstractContainerMenu {
    // THIS YOU HAVE TO DEFINE!
    private static final int TE_INVENTORY_SLOT_COUNT = 0;  // must be the number of slots you have!
    private static final Logger LOGGER = LogUtils.getLogger();

    private final int villagerEntityId;
    private final VillagerProfession profession;
    @Nullable
    private final Inventory playerInventory;

    private MerchantOffers offers = new MerchantOffers();
    private List<InteractionTab> tabs = List.of();
    private int selectedTabIndex;

    // Server side Constructor
    public VillagerInteractionMenu(int containerId, Inventory playerInventory, Villager villager) {
        super(TradeMenus.VILLAGER_INTERACTION_MENU.get(), containerId);
        this.playerInventory = playerInventory;
        this.villagerEntityId = villager.getId();
        this.profession = villager.getVillagerData().getProfession();
        this.offers = villager.getOffers();
        addPlayerSlots();
        refresh();
    }

    // --- CLIENT-SIDE CONSTRUCTOR (from FriendlyByteBuf) ---
    public VillagerInteractionMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        // Client-side MerchantMenu ctor (it’ll create a ClientSideMerchant internally)
        super(TradeMenus.VILLAGER_INTERACTION_MENU.get(), containerId);
        this.playerInventory = playerInventory;
        this.villagerEntityId = buf.readVarInt();

        ResourceLocation key = buf.readResourceLocation();
        VillagerProfession prof = BuiltInRegistries.VILLAGER_PROFESSION.get(key);
        this.profession = (prof != null) ? prof : VillagerProfession.NONE;

        // ✅ Deserialize offers on client
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
            addSlot(new Slot(this.playerInventory, c,
                    invX + c * 20 - 7, invY + 58));
    }

    public void refresh() {
        var level = this.getLevel();
        var villager = this.getClientVillager();
        if (level != null) {
            this.tabs = InteractionTabManager.INSTANCE.resolveTabs(villager, this.offers, level);
        } else {
            this.tabs = List.of();
        }
        setSelectedTabIndex(this.selectedTabIndex);
    }

    @Nullable
    public Level getLevel() {
        if( this.playerInventory == null) {
            return null;
        }
        return this.playerInventory.player.level();
    }

    public MerchantOffers getOffers() {
        return this.offers;
    }

    public List<InteractionTab> getTabs() {
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
    public InteractionTab getSelectedTab() {
        if (this.tabs.isEmpty()) {
            return null;
        }
        int index = Mth.clamp(this.selectedTabIndex, 0, this.tabs.size() - 1);
        return this.tabs.get(index);
    }

    /**
     * CLIENT SIDE ONLY – uses the synced entityId to find the villager in the client world.
     */
    @Nullable
    public Villager getClientVillager() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level instanceof ClientLevel clientLevel) {
            Entity e = clientLevel.getEntity(this.villagerEntityId);
            if (e instanceof Villager villager) {
                return villager;
            }
        }
        return null;
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
        return player.isAlive();
    }

}