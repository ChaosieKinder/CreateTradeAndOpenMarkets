package com.errantmc.markets.screen.villager;

import com.errantmc.markets.npcinteractions.components.InteractionTab;
import com.errantmc.markets.npcinteractions.registry.InteractionTabManager;
import com.errantmc.markets.network.TradeNetwork;
import com.errantmc.markets.network.message.InteractionTabsSyncMessage;
import com.errantmc.markets.npcinteractions.tabs.buy.BuyShopTab;
import com.errantmc.markets.npcinteractions.tabs.sell.SellShopTab;
import com.errantmc.markets.npcinteractions.trading.ShopTrades;
import com.errantmc.markets.economy.Tills;
import com.errantmc.markets.economy.VillageEconomy;
import com.errantmc.markets.screen.TradeMenus;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The menu behind the villager interaction screen. The server opens it in place of vanilla's
 * merchant menu ({@code VillagerInteractionEvents}), and the server's tabs are the only tabs: the
 * client reads them from the open packet and is sent replacements when stock changes. It never
 * works them out for itself, because the server's stock rolls are the ones that count.
 */
public class VillagerInteractionMenu extends AbstractContainerMenu {
    /** Vanilla's merchant menu closes beyond this distance, squared. */
    private static final double MAX_DISTANCE_SQR = 8 * 8;

    private final int villagerEntityId;
    private final VillagerProfession profession;
    private final Inventory playerInventory;
    /** Server side only: the villager being traded with. */
    @Nullable
    private final Villager villager;

    private List<InteractionTab> tabs;
    private int selectedTabIndex;
    /** Client side: the villager's till as the server last sent it. The server reads the villager. */
    private long clientBalance;
    /** Whether the till is the village treasury (a village's banker). Fixed while the menu is open. */
    private final boolean tillIsTreasury;

    /** Server side, opened by {@code VillagerInteractionEvents}. */
    public VillagerInteractionMenu(int containerId, Inventory playerInventory, Villager villager, List<InteractionTab> tabs) {
        super(TradeMenus.VILLAGER_INTERACTION_MENU.get(), containerId);
        this.playerInventory = playerInventory;
        this.villager = villager;
        this.villagerEntityId = villager.getId();
        this.profession = villager.getVillagerData().getProfession();
        this.tabs = List.copyOf(tabs);
        this.tillIsTreasury = Tills.tradesFromTreasury(villager);
        addPlayerSlots();
    }

    /** Client side, from the open packet the server wrote. */
    public VillagerInteractionMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(TradeMenus.VILLAGER_INTERACTION_MENU.get(), containerId);
        this.playerInventory = playerInventory;
        this.villager = null;
        this.villagerEntityId = buf.readVarInt();
        VillagerProfession prof = BuiltInRegistries.VILLAGER_PROFESSION.get(buf.readResourceLocation());
        this.profession = prof != null ? prof : VillagerProfession.NONE;
        this.tabs = InteractionTabManager.INSTANCE.readFromStream(buf);
        this.clientBalance = buf.readVarLong();
        this.tillIsTreasury = buf.readBoolean();
        addPlayerSlots();
    }

    /** Where shop_base draws them: the inventory grid at (61, 115), the hotbar's wider cells below it. */
    private void addPlayerSlots() {
        int invX = 61;
        int invY = 115;
        for (int r = 0; r < 3; ++r)
            for (int c = 0; c < 9; ++c)
                addSlot(new Slot(this.playerInventory, c + r * 9 + 9, invX + c * 18, invY + r * 18));
        for (int c = 0; c < 9; ++c)
            addSlot(new Slot(this.playerInventory, c, invX - 7 + c * 20, invY + 58));
    }

    public Level getLevel() {
        return this.playerInventory.player.level();
    }

    public VillagerProfession getProfession() {
        return this.profession;
    }

    public List<InteractionTab> getTabs() {
        return this.tabs;
    }

    /** Client side: the server's replacement after a trade changed stock and the till. */
    public void update(List<InteractionTab> tabs, long villagerBalance) {
        this.tabs = List.copyOf(tabs);
        this.clientBalance = villagerBalance;
        setSelectedTabIndex(this.selectedTabIndex);
    }

    /** The villager's till, in copper bits. */
    public long getVillagerBalance() {
        return this.villager != null ? Tills.till(this.villager).balance() : this.clientBalance;
    }

    public boolean tillIsTreasury() {
        return this.tillIsTreasury;
    }

    public int getSelectedTabIndex() {
        return this.selectedTabIndex;
    }

    public void setSelectedTabIndex(int selectedTabIndex) {
        this.selectedTabIndex = this.tabs.isEmpty() ? 0 : Mth.clamp(selectedTabIndex, 0, this.tabs.size() - 1);
    }

    @Nullable
    public InteractionTab getSelectedTab() {
        return this.tabs.isEmpty() ? null : this.tabs.get(Mth.clamp(this.selectedTabIndex, 0, this.tabs.size() - 1));
    }

    /**
     * The villager this menu is for, found by its synced entity id in the player's own level. Works
     * on both sides: this is a common class, so it must not reach for {@code Minecraft}, which a
     * dedicated server refuses to load.
     */
    @Nullable
    public Villager getClientVillager() {
        if (this.villager != null) return this.villager;
        return getLevel().getEntity(this.villagerEntityId) instanceof Villager v ? v : null;
    }

    private static final int ENTRIES_PER_TAB = 4096;

    /** The menu button the client sends to trade entry {@code entry} of tab {@code tab}. */
    public static int tradeButton(int tab, int entry) {
        return tab * ENTRIES_PER_TAB + entry;
    }

    /**
     * A click on an entry, on the server: buy it from a buy tab, sell it on a sell tab. Vanilla has
     * already checked the container id and {@link #stillValid}.
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer) || this.villager == null || id < 0) return false;
        int tabIndex = id / ENTRIES_PER_TAB;
        int entryIndex = id % ENTRIES_PER_TAB;
        if (tabIndex >= this.tabs.size()) return false;

        VillageEconomy.settle(this.villager); // a day may have turned while the shop was open
        ShopTrades.Result result = switch (this.tabs.get(tabIndex)) {
            case BuyShopTab buy -> ShopTrades.buy(serverPlayer, this.villager, buy, entryIndex);
            case SellShopTab sell -> ShopTrades.sell(serverPlayer, this.villager, sell, entryIndex);
            default -> ShopTrades.Result.NO_SUCH_ENTRY;
        };
        if (result != ShopTrades.Result.DONE) {
            serverPlayer.displayClientMessage(Component.translatable(result.messageKey()), true);
            return true;
        }
        // Stock and vanilla offer uses changed: saved on the villager, then the client is sent the tabs again
        InteractionTabManager.INSTANCE.saveStock(this.villager, this.tabs, getLevel());
        this.tabs = InteractionTabManager.INSTANCE.resolveTabs(this.villager, this.villager.getOffers(), getLevel());
        TradeNetwork.sendTo(serverPlayer, new InteractionTabsSyncMessage(this.containerId, this.tabs, getVillagerBalance()));
        broadcastChanges();
        return true;
    }

    /** No slots of its own: shift-clicking in the player's inventory does nothing. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    /** As vanilla's merchant menu: open while the villager lives, trades with this player and is near. */
    @Override
    public boolean stillValid(Player player) {
        if (this.villager == null) return true; // the client follows the server
        return this.villager.isAlive()
                && this.villager.getTradingPlayer() == player
                && player.distanceToSqr(this.villager) <= MAX_DISTANCE_SQR;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (this.villager != null && this.villager.getTradingPlayer() == player) {
            this.villager.setTradingPlayer(null);
        }
    }
}
