package net.errantchaosie.tradeandopenmarkets.screen.shop;

import com.mojang.blaze3d.systems.RenderSystem;
import net.errantchaosie.tradeandopenmarkets.TradeAndOpenMarketsMod;
import net.errantchaosie.tradeandopenmarkets.screen.safe.SafeMenu;
import net.errantchaosie.tradeandopenmarkets.util.InventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jetbrains.annotations.Nullable;

/**
 * Replacement shop screen that renders a bespoke background while exposing the
 * active merchant offers and trader information supplied by {@link ShopMenu}.
 */
public class ShopScreen extends AbstractContainerScreen<ShopMenu> {
    private static final ResourceLocation SHOP_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            TradeAndOpenMarketsMod.MOD_ID,
            "textures/gui/shop/shop_base.png"
    );

    private static final int TEXTURE_WIDTH = 276;
    private static final int TEXTURE_HEIGHT = 209;

    private int leftPos;
    private int topPos;

    public ShopScreen(ShopMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - TEXTURE_WIDTH) / 2;
        this.topPos = (this.height - TEXTURE_HEIGHT) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // draw tabs
        InventoryHelper.drawTab(guiGraphics, this.font, "Buy", this.leftPos + 12, this.topPos + 1, mouseX, mouseY, true);
        InventoryHelper.drawTab(guiGraphics, this.font, "Sell", this.leftPos + 51, this.topPos + 1, mouseX, mouseY, false);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);  // 3) draws slot tooltips on top
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);


        // draw main background
        RenderSystem.setShaderTexture(0, SHOP_TEXTURE);
        guiGraphics.blit(SHOP_TEXTURE, this.leftPos, this.topPos,
                0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        // draw shop title - this is the profession of the shop
        guiGraphics.drawString(this.font, this.menu.getTitle(),
                this.leftPos + 18, this.topPos + 19, 0x404040, false);

        //renderTraderDetails(guiGraphics);
        //renderOfferSummary(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        //super.renderLabels(guiGraphics, mouseX, mouseY);
        //renderTraderDetails(guiGraphics);
        //renderOfferSummary(guiGraphics);
    }



    private void renderTraderDetails(GuiGraphics guiGraphics) {
        @Nullable Merchant trader = this.menu.getTrader();
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
                "screen.%s.shop.offers".formatted(TradeAndOpenMarketsMod.MOD_ID),
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