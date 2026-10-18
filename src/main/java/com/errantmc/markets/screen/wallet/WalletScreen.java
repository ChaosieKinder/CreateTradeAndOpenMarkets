package com.errantmc.markets.screen.wallet;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.errantmc.markets.lib.ui.component.ItemButton;
import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.domain.Purse;
import com.errantmc.markets.component.Purses;
import com.errantmc.markets.domain.CoinDenomination;
import com.errantmc.markets.domain.MoneyFormat;
import com.errantmc.markets.item.CoinItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class WalletScreen extends AbstractContainerScreen<WalletMenu> {

    // keep button refs for live enable/disable
    ArrayList<ItemButton> buttons = new ArrayList<>();
    private Button btnDepositAll;

    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ErrantTrade.MOD_ID, "textures/gui/wallet/wallet_screen.png");

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
        buttons.clear();    // clear if we've called init already
        // Small icon buttons (you can use ImageButton with your own icons)
        // Here I'll use vanilla-looking text buttons for clarity; replace with ImageButton for icons.
        // Withdraw (-1) buttons; replace with ImageButton if you have icon textures

        int left = this.leftPos + 130;
        int top  = this.topPos + 48;
        int x = left + 8;
        int y = top + 12;
        int rowStartY = y;
        int rowCount = 0;
        for (CoinDenomination coinDenom : CoinDenomination.coins()) {
            var item = CoinItem.createCurrencyItem(coinDenom, 1);
            var menuId = WalletMenu.withdrawButton(coinDenom);
            var button = new ItemButton(x, y, 40, 16, () -> item, b -> sendMenuButton(menuId), item.getDisplayName());
            button.setTooltip(Tooltip.create(
                    Component.translatable("item.erranttrade."+ coinDenom.id)
                            .append("\n")
                            .append(Component.translatable("item.erranttrade."+ coinDenom.id + ".tooltip"))
                            .append("\n")
                            .append(Component.translatable("item.erranttrade.wallet.withdraw.tooltip"))
            ));
            button.setScale(0.8f, 0.8f);
            button.setImageOffset(ItemButton.ItemAnchor.LEFT, 0, 0);
            button.setTextOffset(ItemButton.ItemAnchor.RIGHT, 0, 0);
            button.setMessage(Component.literal("0"));
            buttons.add(button);
            addRenderableWidget(button);
            x += 21 + 16 + 6;
            y += 8;
            rowCount++;
            if(rowCount % 2 == 0) {
                rowStartY += 18;
                x = left + 8;
                y = rowStartY;
            }
        }
        y += 8;
        // Deposit All
        btnDepositAll = addRenderableWidget(
                Button.builder(Component.translatable("item.erranttrade.wallet.deposit_all"),
                                b -> sendMenuButton(WalletMenu.BTN_DEPOSIT_ALL))
                        .pos(this.leftPos + 138, this.topPos + 150).size(70, 10)
                        .tooltip(Tooltip.create(Component.translatable("item.erranttrade.wallet.deposit_all.tooltip"))).build()
        );
    }

    /** Send container button click to server. */
    private void sendMenuButton(int id) {
        boolean shift = net.minecraft.client.gui.screens.Screen.hasShiftDown();
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId , id+ (shift ? 1000 : 0));
        }
    }

    /** Live-enable/disable withdraw buttons based on current counts; runs each tick client-side. */
    @Override
    protected void containerTick() {
        super.containerTick();

        var mc = this.minecraft;
        var walletStack = (mc != null && mc.player != null)
                ? mc.player.getItemInHand(this.menu.getHand())
                : ItemStack.EMPTY;

        Purse c = Purses.get(walletStack); // safe client read

        for (ItemButton btn : buttons) {
            if(btn.getIconItem().getItem() instanceof CoinItem coin) {
                int value = c.count(coin.denom());
                btn.active = value > 0;
                btn.setMessage(Component.literal("" + value));

            }
        }
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
        super.render(gg, mouseX, mouseY, partialTicks); // 2) draws slots & items
        this.renderTooltip(gg, mouseX, mouseY);  // 3) draws slot tooltips on top
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float mouseX, int mouseY, int partialTick) {
        // Draw Background
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        guiGraphics.blit(GUI_TEXTURE,
                (this.width - this.imageWidth) / 2,
                (this.height - this.imageHeight) / 2,
                0, 0, this.imageWidth, this.imageHeight);

        // Draw Total Value at top right of wallet
        String totalValueText = MoneyFormat.format(Purses.get(currentWalletStack()).totalValue());
        int walletTotalX = leftPos + imageWidth - 40 - this.font.width(totalValueText)/2;
        int walletTotalY = topPos + 48 - this.font.lineHeight/2;
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(walletTotalX, walletTotalY, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(10f));
        guiGraphics.drawCenteredString(font, totalValueText,
                0, 0,
                0xFFE0A0);
        pose.popPose();
        // Buttons and itemstacks are registered in init and handled as sub-components
    }

    // WalletScreen: helper to read the current wallet stack from the hand
    private ItemStack currentWalletStack() {
        var mc = this.minecraft;
        if (mc == null || mc.player == null) return ItemStack.EMPTY;
        return mc.player.getItemInHand(this.menu.getHand());
    }

}