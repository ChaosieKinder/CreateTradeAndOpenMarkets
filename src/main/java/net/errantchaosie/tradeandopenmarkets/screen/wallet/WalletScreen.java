package net.errantchaosie.tradeandopenmarkets.screen.wallet;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.errantchaosie.tradeandopenmarkets.TradeAndOpenMarketsMod;
import net.errantchaosie.tradeandopenmarkets.component.CurrencyData;
import net.errantchaosie.tradeandopenmarkets.component.WalletBalance;
import net.errantchaosie.tradeandopenmarkets.data.CoinDenomination;
import net.errantchaosie.tradeandopenmarkets.item.TAOMItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import javax.tools.Tool;

public class WalletScreen extends AbstractContainerScreen<WalletMenu> {

    // keep button refs for live enable/disable
    private Button btnEmerald, btnPlatinum, btnElectrum, btnGold, btnSilver, btnBronze, btnCopper, btnDepositAll;

    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TradeAndOpenMarketsMod.MOD_ID, "textures/gui/wallet/wallet_screen.png");

    public WalletScreen(WalletMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 256;
        this.titleLabelX = 0;
        this.titleLabelY = 0;
        this.inventoryLabelX = 47;
        this.inventoryLabelY = this.imageHeight - 104;
    }

    @Override
    protected void init() {
        super.init();
        int left = this.leftPos;
        int top  = this.topPos;

        // Small icon buttons (you can use ImageButton with your own icons)
        // Here I'll use vanilla-looking text buttons for clarity; replace with ImageButton for icons.
        // Withdraw (-1) buttons; replace with ImageButton if you have icon textures
        btnEmerald  = addRenderableWidget(button(left + 8,  top + 18, "-", WalletMenu.BTN_WITHDRAW_EMERALD));
        btnPlatinum = addRenderableWidget(button(left + 8,  top + 36, "-", WalletMenu.BTN_WITHDRAW_PLATINUM));
        btnElectrum = addRenderableWidget(button(left + 8,  top + 54, "-", WalletMenu.BTN_WITHDRAW_ELECTRUM));
        btnGold     = addRenderableWidget(button(left + 8,  top + 72, "-", WalletMenu.BTN_WITHDRAW_GOLD));
        btnSilver   = addRenderableWidget(button(left + 8,  top + 90, "-", WalletMenu.BTN_WITHDRAW_SILVER));
        btnBronze   = addRenderableWidget(button(left + 8,  top +108, "-", WalletMenu.BTN_WITHDRAW_BRONZE));
        btnCopper   = addRenderableWidget(button(left + 8,  top +126, "-", WalletMenu.BTN_WITHDRAW_COPPER));

        // Deposit All
        btnDepositAll = addRenderableWidget(
                Button.builder(Component.translatable("item.tradeandopenmarkets.wallet.deposit_all"),
                                b -> sendMenuButton(WalletMenu.BTN_DEPOSIT_ALL))
                        .pos(left + 8, top + 144).size(80, 16)
                        .tooltip(Tooltip.create(Component.translatable("item.tradeandopenmarkets.wallet.deposit_all.tooltip"))).build()
        );
    }

    private Button button(int x, int y, String label, int id) {
        return Button.builder(Component.literal(label), b -> sendMenuButton(id))
                .pos(x, y).size(12, 12)
                .tooltip(Tooltip.create(Component.translatable("Click to add one to inventory. Shift+Click to add a stack."))).build();
    }

    /** Send container button click to server. */
    private void sendMenuButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    /** Live-enable/disable withdraw buttons based on current counts; runs each tick client-side. */
    @Override
    protected void containerTick() {
        super.containerTick();
        CurrencyData c = WalletBalance.get(this.menu.getWalletStack()); // safe client read

        btnEmerald.active  = c.coinsEmerald()  > 0;
        btnPlatinum.active = c.coinsPlatinum() > 0;
        btnElectrum.active = c.coinsElectrum() > 0;
        btnGold.active     = c.coinsGold()     > 0;
        btnSilver.active   = c.coinsSilver()   > 0;
        btnBronze.active   = c.coinsBronze()   > 0;
        btnCopper.active   = c.coinsCopper()   > 0;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        int x =  (this.width - this.imageWidth) / 2;
        int y =  (this.height - this.imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Render coin icons + counts
        int xIcon = leftPos + 24;
        int xText = xIcon + 18;
        y = topPos + 16;
        PoseStack pose = guiGraphics.pose();
        CurrencyData c = WalletBalance.get(currentWalletStack()); // expose a getter in menu for walletStack

        // draw from highest to lowest
        drawLine(guiGraphics, xIcon, y,      new ItemStack(TAOMItems.COIN_EMERALD.get()),  c.coinsEmerald());
        y += 18;
        drawLine(guiGraphics, xIcon, y,      new ItemStack(TAOMItems.COIN_PLATINUM.get()), c.coinsPlatinum());
        y += 18;
        drawLine(guiGraphics, xIcon, y,      new ItemStack(TAOMItems.COIN_ELECTRUM.get()), c.coinsElectrum());
        y += 18;
        drawLine(guiGraphics, xIcon, y,      new ItemStack(TAOMItems.COIN_GOLD.get()),     c.coinsGold());
        y += 18;
        drawLine(guiGraphics, xIcon, y,      new ItemStack(TAOMItems.COIN_SILVER.get()),   c.coinsSilver());
        y += 18;
        drawLine(guiGraphics, xIcon, y,      new ItemStack(TAOMItems.COIN_BRONZE.get()),   c.coinsBronze());
        y += 18;
        drawLine(guiGraphics, xIcon, y,      new ItemStack(TAOMItems.COIN_COPPER.get()),   c.coinsCopper());

        // Grand total (top-right)
        long total = c.totalValue(); // see helper below
        String pretty = java.text.NumberFormat.getIntegerInstance().format(total);
        guiGraphics.drawCenteredString(font, Component.translatable("item.tradeandopenmarkets.wallet.total_prefix", pretty),
                leftPos + imageWidth - 8 - this.font.width(pretty) - 40, topPos + 6, 0xFFE0A0);
    }

    // WalletScreen: helper to read the current wallet stack from the hand
    private ItemStack currentWalletStack() {
        var mc = this.minecraft;
        if (mc == null || mc.player == null) return ItemStack.EMPTY;
        return mc.player.getItemInHand(this.menu.getHand());
    }

    private void drawLine(GuiGraphics guiGraphics, int xIcon, int y, ItemStack iconStack, int count) {
        // icon
        guiGraphics.renderFakeItem(iconStack, xIcon, y);
        // count text
        String text = String.valueOf(count);
        drawScaled(guiGraphics, text, xIcon+  18, y + 4,0.7f ,0xFFFFFF);
    }


    private void drawScaled(GuiGraphics gg, String text, float x, float y, float scale, int color) {
        var pose = gg.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1f);
        gg.drawString(this.font, text, 0, 0, color, false);
        pose.popPose();
    }
}