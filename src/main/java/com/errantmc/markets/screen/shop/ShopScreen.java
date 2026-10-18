package com.errantmc.markets.screen.shop;

import com.mojang.blaze3d.systems.RenderSystem;
import com.errantmc.markets.ErrantTrade;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Replacement trading screen, drawn over vanilla's own {@link MerchantMenu}.
 *
 * <p>It must be that menu, not a client-side copy of it: the server runs vanilla's
 * {@code MerchantMenu}, and every click a screen sends is applied to the server's menu by slot
 * index (BACKLOG #18). That also means the slots sit where vanilla puts them; a layout of our own
 * needs a server-side menu of our own, which is the trader overhaul's job. Until then the screen
 * is off by default ({@code ClientConfig.REPLACE_TRADING_SCREEN}).
 */
public class ShopScreen extends AbstractContainerScreen<MerchantMenu> {
    private static final ResourceLocation SHOP_TEXTURE = texture("textures/gui/shop/shop_base.png");
    private static final ResourceLocation TAB = texture("textures/gui/components/tab_deselected.png");
    private static final ResourceLocation TAB_SELECTED = texture("textures/gui/components/tab_selected.png");
    private static final ResourceLocation TAB_HOVER = texture("textures/gui/components/tab_hover.png");
    private static final int TEXTURE_WIDTH = 276;
    private static final int TEXTURE_HEIGHT = 209;
    private static final int TAB_WIDTH = 38;
    private static final int TAB_HEIGHT = 17;

    public ShopScreen(MerchantMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = TEXTURE_WIDTH;
        this.imageHeight = TEXTURE_HEIGHT;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        drawTab(guiGraphics, this.font, Component.translatable("screen.erranttrade.shop.buy"),
                this.leftPos + 12, this.topPos + 1, mouseX, mouseY, true);
        drawTab(guiGraphics, this.font, Component.translatable("screen.erranttrade.shop.sell"),
                this.leftPos + 51, this.topPos + 1, mouseX, mouseY, false);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        guiGraphics.blit(SHOP_TEXTURE, this.leftPos, this.topPos,
                0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        // The shop's title is the trader's profession
        guiGraphics.drawString(this.font, this.title, this.leftPos + 18, this.topPos + 19, 0x404040, false);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // The title is drawn with the background; the trade list is still to come
    }

    /** Not drawn yet: a count of the trader's offers, for the trade list to come. */
    @SuppressWarnings("unused")
    private void renderOfferSummary(GuiGraphics guiGraphics) {
        MerchantOffers offers = this.menu.getOffers();
        int outOfStock = 0;
        for (MerchantOffer offer : offers) {
            if (offer.isOutOfStock()) outOfStock++;
        }
        Component summary = Component.translatable("screen.erranttrade.shop.offers", offers.size(), outOfStock);
        guiGraphics.drawString(this.font, summary, this.leftPos + 12, this.topPos + 34, 0x404040, false);
    }

    private static void drawTab(GuiGraphics guiGraphics, Font font, Component text,
                                int x, int y, int mouseX, int mouseY, boolean selected) {
        boolean hovered = mouseX >= x && mouseX <= x + TAB_WIDTH && mouseY >= y && mouseY <= y + TAB_HEIGHT;
        ResourceLocation texture = hovered ? TAB_HOVER : selected ? TAB_SELECTED : TAB;
        guiGraphics.blit(texture, x, y, 0, 0, TAB_WIDTH, TAB_HEIGHT, TAB_WIDTH, TAB_HEIGHT);
        guiGraphics.drawString(font, text, x + 8, y + 5, 0xFFFFFF, false);
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(ErrantTrade.MOD_ID, path);
    }
}
