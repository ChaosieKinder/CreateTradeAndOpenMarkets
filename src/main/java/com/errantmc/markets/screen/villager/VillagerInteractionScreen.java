package com.errantmc.markets.screen.villager;

import com.mojang.blaze3d.systems.RenderSystem;
import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.component.Purses;
import com.errantmc.markets.domain.MoneyFormat;
import com.errantmc.markets.item.wallet.WalletFinder;
import com.errantmc.markets.npcinteractions.components.InteractionTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;

/**
 * Replacement npcinteractions screen that renders a bespoke background while exposing the
 * active merchant offers and trader information supplied by {@link VillagerInteractionMenu}.
 */
public class VillagerInteractionScreen extends AbstractContainerScreen<VillagerInteractionMenu> {

    private static final ResourceLocation SHOP_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ErrantTrade.MOD_ID,
            "textures/gui/npcinteractions/shop_base.png"
    );

    public static final int SCREEN_WIDTH = 276;
    public static final int SCREEN_HEIGHT = 209;
    private static final int TAB_START_X = 12;
    private static final int TAB_START_Y = 1;
    private static final int TAB_SPACING = 39;
    @Nullable
    private final Villager villager;

    private int leftPos;
    private int topPos;

    public VillagerInteractionScreen(VillagerInteractionMenu menu, Inventory playerInventory, Component otitle) {
        super(menu, playerInventory, otitle);
        villager = menu.getClientVillager();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        //this.menu.refresh();
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - SCREEN_WIDTH) / 2;
        this.topPos = (this.height - SCREEN_HEIGHT) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTabs(guiGraphics, mouseX, mouseY);
        // Render Active Tab
        var tab = this.menu.getSelectedTab();
        if(tab != null) {
            tab.renderPanel(guiGraphics, this.leftPos, this.topPos, mouseX, mouseY);
        }
        // Render Tooltip
        this.renderTooltip(guiGraphics, mouseX, mouseY);  // 3) draws slot tooltips on top
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);


        // draw main background
        RenderSystem.setShaderTexture(0, SHOP_TEXTURE);
        guiGraphics.blit(SHOP_TEXTURE, this.leftPos, this.topPos,
                0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, SCREEN_WIDTH, SCREEN_HEIGHT);

        Component title = villager != null ? villager.getDisplayName() : this.getTitle();
        // draw npcinteractions title - this is the profession of the npcinteractions
        guiGraphics.drawString(this.font, title,
                this.leftPos + 18, this.topPos + 19, 0x404040, false);

        renderWalletBalance(guiGraphics);
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
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int getTabIndexAt(double mouseX, double mouseY) {
        int x = this.leftPos + TAB_START_X;
        int y = this.topPos + TAB_START_Y;
        for (int i = 0; i < this.menu.getTabs().size(); i++) {
            if (mouseX >= x && mouseX <= x + 38 && mouseY >= y && mouseY <= y + 17) {
                return i;
            }
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
            boolean isSelected = i == selected;
            tab.renderTab(guiGraphics, this.font, tab.title(), x, y, mouseX, mouseY, isSelected);
            x += TAB_SPACING;
        }
    }

    private void renderWalletBalance(GuiGraphics guiGraphics) {
        Player player = this.minecraft != null ? this.minecraft.player : null;
        ItemStack wallet = WalletFinder.findWallet(player);

        Component text;
        if (wallet.isEmpty()) {
            text = Component.translatable("screen.%s.npcinteractions.wallet_missing".formatted(ErrantTrade.MOD_ID));
        } else {
            String formatted = MoneyFormat.format(Purses.get(wallet).totalValue());
            text = Component.translatable("screen.%s.npcinteractions.wallet_balance".formatted(ErrantTrade.MOD_ID), formatted);
        }

        int textWidth = this.font.width(text);
        int x = this.leftPos + SCREEN_WIDTH - 18 - textWidth;
        int y = this.topPos + 19;
        guiGraphics.drawString(this.font, text, x, y, 0x404040, false);
    }



    private void renderTraderDetails(GuiGraphics guiGraphics) {
        @Nullable Merchant trader = this.menu.getClientVillager();
        if (trader == null) {
            return;
        }

        Component traderName = resolveTraderName(trader);
        if (traderName == null) {
            return;
        }
        int x = this.leftPos + 12;
        int y = this.topPos + 20;
        guiGraphics.drawString(this.font, traderName, x, y, 0x404040, false);
    }

    private void renderOfferSummary(GuiGraphics guiGraphics) {
        MerchantOffers offers = this.menu.getOffers();
        int offerCount = offers.size();
        int outOfStock = 0;
        for (MerchantOffer offer : offers) {
            if (offer.isOutOfStock()) {
                outOfStock++;
            }
        }

        int x = this.leftPos + 12;
        int y = this.topPos + 34;
        Component summary = Component.translatable(
                "screen.%s.npcinteractions.offers".formatted(ErrantTrade.MOD_ID),
                offerCount,
                outOfStock
        );
        guiGraphics.drawString(this.font, summary, x, y, 0x404040, false);
    }

    @Nullable
    private Component resolveTraderName(Merchant trader) {
        if (trader instanceof Nameable nameable) {
            return nameable.getDisplayName();
        }

        return null;
    }

    @Override
    public void onClose() {
        super.onClose();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.closeContainer();
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}