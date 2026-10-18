package com.errantmc.markets.item.idcard;

import com.errantmc.markets.component.IdentityCardData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IdentityCardTooltipClient implements ClientTooltipComponent {
    private static final int BORDER = 4;
    private static final int FACE_SIZE = 16;
    private static final int SPACING = 6;

    private final ItemStack headStack;
    private final net.minecraft.network.chat.Component ownerLine;
    private final net.minecraft.network.chat.Component createdLine;

    public IdentityCardTooltipClient(IdentityCardTooltip tooltip) {
        this.ownerLine = tooltip.ownerLine();
        this.createdLine = tooltip.createdLine();
        this.headStack = createHeadStack(tooltip.data());
    }

    @Override
    public int getHeight() {
        return FACE_SIZE + BORDER * 2;
    }

    @Override
    public int getWidth(Font font) {
        int textWidth = Math.max(font.width(ownerLine), font.width(createdLine));
        return BORDER * 2 + FACE_SIZE + SPACING + textWidth;
    }

    @Override
    public void renderImage(Font font, int mouseX, int mouseY, GuiGraphics guiGraphics) {
        int faceX = mouseX + BORDER;
        int faceY = mouseY + BORDER;
        guiGraphics.renderItem(headStack, faceX, faceY);
        guiGraphics.renderItemDecorations(font, headStack, faceX, faceY);
    }

    private static ItemStack createHeadStack(IdentityCardData data) {
        return data.createHeadStack();
    }
}