package net.errantchaosie.tradeandopenmarkets.screen.safe;

import com.mojang.blaze3d.systems.RenderSystem;
import net.errantchaosie.tradeandopenmarkets.TradeAndOpenMarketsMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class SafeScreen extends AbstractContainerScreen<SafeMenu> {


    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TradeAndOpenMarketsMod.MOD_ID, "textures/gui/safe/safe_screen.png");

    public SafeScreen(SafeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 256;
        this.imageWidth = 256;
        this.titleLabelX = 38;
        this.titleLabelY = 5;
        this.inventoryLabelX = 48;
        this.inventoryLabelY = 142;
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
