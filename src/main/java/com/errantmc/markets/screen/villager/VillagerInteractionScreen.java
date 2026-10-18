package com.errantmc.markets.screen.villager;

import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.component.Purses;
import com.errantmc.markets.domain.MoneyFormat;
import com.errantmc.markets.item.PlayerMoney;
import com.errantmc.markets.item.wallet.WalletFinder;
import com.errantmc.markets.npcinteractions.components.InteractionTab;
import com.errantmc.markets.npcinteractions.components.SimpleShopTab;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * The villager's shop: tabs along the top, the selected tab's 3x9 grid in the brown panel, the
 * player's inventory below. Shop tabs page through their entries 27 at a time.
 */
public class VillagerInteractionScreen extends AbstractContainerScreen<VillagerInteractionMenu> {

    private static final ResourceLocation SHOP_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ErrantTrade.MOD_ID, "textures/gui/npcinteractions/shop_base.png");

    public static final int SCREEN_WIDTH = 276;
    public static final int SCREEN_HEIGHT = 209;
    private static final int TAB_START_X = 12;
    private static final int TAB_START_Y = 1;
    private static final int TAB_SPACING = 39;
    /** The strip under the grid, in screen space: page arrows and the page number. */
    private static final int PAGER_Y = SimpleShopTab.PANEL_Y + 66;
    private static final int PAGER_PREV_X = SimpleShopTab.PANEL_X + 44, PAGER_NEXT_X = SimpleShopTab.PANEL_X + 192;
    private static final int ARROW_WIDTH = 10;

    @Nullable
    private final Villager villager;
    /** Page per tab index. Kept here because the server replaces the tabs after every trade. */
    private final Map<Integer, Integer> pages = new HashMap<>();

    public VillagerInteractionScreen(VillagerInteractionMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = SCREEN_WIDTH;
        this.imageHeight = SCREEN_HEIGHT;
        this.villager = menu.getClientVillager();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTabs(guiGraphics, mouseX, mouseY);
        InteractionTab tab = this.menu.getSelectedTab();
        if (tab instanceof SimpleShopTab shop) {
            int page = page(shop);
            shop.renderGrid(guiGraphics, this.font, this.leftPos, this.topPos, mouseX, mouseY, page);
            renderPager(guiGraphics, shop, page, mouseX, mouseY);
            renderEntryTooltip(guiGraphics, shop, page, mouseX, mouseY);
        } else if (tab != null) {
            tab.renderPanel(guiGraphics, this.leftPos, this.topPos, mouseX, mouseY);
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY); // the inventory's own slot tooltips
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        guiGraphics.blit(SHOP_TEXTURE, this.leftPos, this.topPos, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, SCREEN_WIDTH, SCREEN_HEIGHT);
        Component title = this.villager != null ? this.villager.getDisplayName() : this.getTitle();
        guiGraphics.drawString(this.font, title, this.leftPos + 18, this.topPos + 19, 0x404040, false);
        renderWalletBalance(guiGraphics);
        // The villager's till (a village banker's is the treasury), centred between its name and the wallet
        String tillKey = this.menu.tillIsTreasury() ? "screen.erranttrade.npcinteractions.treasury" : "screen.erranttrade.npcinteractions.till";
        Component till = Component.translatable(tillKey, MoneyFormat.format(this.menu.getVillagerBalance()));
        guiGraphics.drawString(this.font, till, this.leftPos + (SCREEN_WIDTH - this.font.width(till)) / 2, this.topPos + 19, 0x404040, false);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int tabIndex = getTabIndexAt(mouseX, mouseY);
            if (tabIndex >= 0) {
                this.menu.setSelectedTabIndex(tabIndex);
                return true;
            }
            if (this.menu.getSelectedTab() instanceof SimpleShopTab shop) {
                if (clickPager(shop, mouseX, mouseY)) return true;
                // A click on an entry asks the server to trade it; the server answers with new tabs
                int entry = shop.entryAt(this.leftPos, this.topPos, mouseX, mouseY, page(shop));
                if (entry >= 0 && this.minecraft != null && this.minecraft.gameMode != null) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId,
                            VillagerInteractionMenu.tradeButton(this.menu.getSelectedTabIndex(), entry));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.menu.getSelectedTab() instanceof SimpleShopTab shop && shop.pageCount() > 1 && scrollY != 0) {
            setPage(shop, page(shop) + (scrollY < 0 ? 1 : -1));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ---------- pages ----------

    private int page(SimpleShopTab shop) {
        int page = this.pages.getOrDefault(this.menu.getSelectedTabIndex(), 0);
        return Math.min(page, shop.pageCount() - 1); // the shop may have shrunk since
    }

    private void setPage(SimpleShopTab shop, int page) {
        this.pages.put(this.menu.getSelectedTabIndex(), Math.floorMod(page, shop.pageCount()));
    }

    private void renderPager(GuiGraphics guiGraphics, SimpleShopTab shop, int page, int mouseX, int mouseY) {
        if (shop.pageCount() <= 1) return;
        int y = this.topPos + PAGER_Y;
        drawArrow(guiGraphics, "<", this.leftPos + PAGER_PREV_X, y, mouseX, mouseY);
        drawArrow(guiGraphics, ">", this.leftPos + PAGER_NEXT_X, y, mouseX, mouseY);
        Component label = Component.translatable("screen.erranttrade.npcinteractions.page", page + 1, shop.pageCount());
        int centre = this.leftPos + SimpleShopTab.PANEL_X + SimpleShopTab.PANEL_TEXTURE_WIDTH / 2;
        guiGraphics.drawString(this.font, label, centre - this.font.width(label) / 2, y, 0xE0D0B0, false);
    }

    private void drawArrow(GuiGraphics guiGraphics, String arrow, int x, int y, int mouseX, int mouseY) {
        boolean hovered = over(mouseX, mouseY, x, y);
        guiGraphics.drawString(this.font, arrow, x + 2, y, hovered ? 0xFFFFA0 : 0xE0D0B0, false);
    }

    private boolean clickPager(SimpleShopTab shop, double mouseX, double mouseY) {
        if (shop.pageCount() <= 1) return false;
        int y = this.topPos + PAGER_Y;
        if (over(mouseX, mouseY, this.leftPos + PAGER_PREV_X, y)) {
            setPage(shop, page(shop) - 1);
            return true;
        }
        if (over(mouseX, mouseY, this.leftPos + PAGER_NEXT_X, y)) {
            setPage(shop, page(shop) + 1);
            return true;
        }
        return false;
    }

    private boolean over(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + ARROW_WIDTH && mouseY >= y - 2 && mouseY < y + this.font.lineHeight + 2;
    }

    // ---------- tooltips ----------

    private void renderEntryTooltip(GuiGraphics guiGraphics, SimpleShopTab shop, int page, int mouseX, int mouseY) {
        int index = shop.entryAt(this.leftPos, this.topPos, mouseX, mouseY, page);
        if (index < 0 || this.minecraft == null || this.minecraft.player == null) return;
        var entry = shop.entries().get(index);
        Player player = this.minecraft.player;
        SimpleShopTab.TradeView view = new SimpleShopTab.TradeView() {
            @Override public long money() {
                return PlayerMoney.loose(player).totalValue() + PlayerMoney.wallet(player).totalValue();
            }
            @Override public long villagerMoney() {
                return VillagerInteractionScreen.this.menu.getVillagerBalance();
            }
            @Override public int count(ItemStack item) {
                int n = 0;
                for (ItemStack s : player.getInventory().items) if (ItemStack.isSameItem(s, item)) n += s.getCount();
                for (ItemStack s : player.getInventory().offhand) if (ItemStack.isSameItem(s, item)) n += s.getCount();
                return n;
            }
        };
        var lines = shop.tooltip(entry, Screen.getTooltipFromItem(this.minecraft, entry.item()), view);
        guiGraphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    // ---------- tabs and header ----------

    private int getTabIndexAt(double mouseX, double mouseY) {
        int x = this.leftPos + TAB_START_X;
        int y = this.topPos + TAB_START_Y;
        for (int i = 0; i < this.menu.getTabs().size(); i++) {
            if (mouseX >= x && mouseX <= x + 38 && mouseY >= y && mouseY <= y + 17) return i;
            x += TAB_SPACING;
        }
        return -1;
    }

    private void renderTabs(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = this.leftPos + TAB_START_X;
        int y = this.topPos + TAB_START_Y;
        int selected = this.menu.getSelectedTabIndex();
        for (int i = 0; i < this.menu.getTabs().size(); i++) {
            InteractionTab tab = this.menu.getTabs().get(i);
            tab.renderTab(guiGraphics, this.font, tab.title(), x, y, mouseX, mouseY, i == selected);
            x += TAB_SPACING;
        }
    }

    private void renderWalletBalance(GuiGraphics guiGraphics) {
        Player player = this.minecraft != null ? this.minecraft.player : null;
        ItemStack wallet = WalletFinder.findWallet(player);
        Component text = wallet.isEmpty()
                ? Component.translatable("screen.erranttrade.npcinteractions.wallet_missing")
                : Component.translatable("screen.erranttrade.npcinteractions.wallet_balance",
                        MoneyFormat.format(Purses.get(wallet).totalValue()));
        int x = this.leftPos + SCREEN_WIDTH - 18 - this.font.width(text);
        guiGraphics.drawString(this.font, text, x, this.topPos + 19, 0x404040, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
