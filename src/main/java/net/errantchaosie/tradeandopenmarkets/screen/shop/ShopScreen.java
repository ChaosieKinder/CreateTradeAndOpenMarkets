package net.errantchaosie.tradeandopenmarkets.screen.shop;

import com.mojang.blaze3d.systems.RenderSystem;
import net.errantchaosie.tradeandopenmarkets.TradeAndOpenMarketsMod;
import net.errantchaosie.tradeandopenmarkets.component.WalletBalance;
import net.errantchaosie.tradeandopenmarkets.item.wallet.WalletFinder;
import net.errantchaosie.tradeandopenmarkets.shop.tab.ShopEntry;
import net.errantchaosie.tradeandopenmarkets.shop.tab.ShopTab;
import net.errantchaosie.tradeandopenmarkets.shop.tab.SimpleShopTab;
import net.errantchaosie.tradeandopenmarkets.util.InventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;

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
    private static final int TAB_START_X = 12;
    private static final int TAB_START_Y = 1;
    private static final int TAB_SPACING = 39;

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance();

    private int leftPos;
    private int topPos;

    public ShopScreen(ShopMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.menu.refresh();
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - TEXTURE_WIDTH) / 2;
        this.topPos = (this.height - TEXTURE_HEIGHT) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTabs(guiGraphics, mouseX, mouseY);
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

        renderWalletBalance(guiGraphics);
        renderActiveTab(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        //super.renderLabels(guiGraphics, mouseX, mouseY);
        //renderTraderDetails(guiGraphics);
        //renderOfferSummary(guiGraphics);
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
            ShopTab tab = this.menu.getTabs().get(i);
            boolean isSelected = i == selected;
            InventoryHelper.drawTab(guiGraphics, this.font, tab.title(), x, y, mouseX, mouseY, isSelected);
            x += TAB_SPACING;
        }
    }

    private void renderActiveTab(GuiGraphics guiGraphics) {
        ShopTab tab = this.menu.getSelectedTab();
        if (tab instanceof SimpleShopTab simple) {
            renderSimpleTab(guiGraphics, simple);
        }
    }

    private void renderSimpleTab(GuiGraphics guiGraphics, SimpleShopTab tab) {
        int x = this.leftPos + 18;
        int y = this.topPos + 44;
        int index = 0;
        for (ShopEntry entry : tab.entries()) {
            if (index >= 6) {
                break;
            }
            guiGraphics.renderItem(entry.item(), x, y);
            guiGraphics.renderItemDecorations(this.font, entry.item(), x, y);
            Component priceText = Component.literal("Price: " + entry.price());
            Component stockText = entry.hasLimitedStock()
                    ? Component.literal("Stock: " + entry.stock())
                    : Component.literal("Stock: ∞");
            int textX = x + 20;
            guiGraphics.drawString(this.font, entry.item().getHoverName(), textX, y + 4, 0x404040, false);
            guiGraphics.drawString(this.font, priceText, textX, y + 16, 0x202020, false);
            guiGraphics.drawString(this.font, stockText, textX + 70, y + 16, 0x202020, false);
            y += 28;
            index++;
        }
    }

    private void renderWalletBalance(GuiGraphics guiGraphics) {
        Player player = this.minecraft != null ? this.minecraft.player : null;
        ItemStack wallet = WalletFinder.findWallet(player);

        Component text;
        if (wallet.isEmpty()) {
            text = Component.translatable("screen.%s.shop.wallet_missing".formatted(TradeAndOpenMarketsMod.MOD_ID));
        } else {
            double balance = WalletBalance.get(wallet).totalValue();
            String formatted = CURRENCY_FORMAT.format(balance);
            text = Component.translatable("screen.%s.shop.wallet_balance".formatted(TradeAndOpenMarketsMod.MOD_ID), formatted);
        }

        int textWidth = this.font.width(text);
        int x = this.leftPos + TEXTURE_WIDTH - 18 - textWidth;
        int y = this.topPos + 19;
        guiGraphics.drawString(this.font, text, x, y, 0x404040, false);
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