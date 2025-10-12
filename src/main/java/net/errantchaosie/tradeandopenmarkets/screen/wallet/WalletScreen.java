package net.errantchaosie.tradeandopenmarkets.screen.wallet;

import com.mojang.blaze3d.systems.RenderSystem;
import net.errantchaosie.tradeandopenmarkets.TradeAndOpenMarketsMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class WalletScreen extends AbstractContainerScreen<WalletMenu> {


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
    protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);

        int x =  (this.width - this.imageWidth) / 2;
        int y =  (this.height - this.imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }
}