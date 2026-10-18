package com.errantmc.markets.npcinteractions.components;

import com.mojang.blaze3d.systems.RenderSystem;
import com.errantmc.markets.ErrantTrade;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Represents a concrete tabs shown in the npcinteractions screen.
 */
public abstract class InteractionTab {

    private static final int TAB_WIDTH = 38;
    private static final int TAB_HEIGHT = 17;
    private static final ResourceLocation TAB_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ErrantTrade.MOD_ID,
            "textures/gui/components/tab_deselected.png"
    );
    private static final ResourceLocation TAB_SELECTED_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ErrantTrade.MOD_ID,
            "textures/gui/components/tab_selected.png"
    );
    private static final ResourceLocation TAB_HOVER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ErrantTrade.MOD_ID,
            "textures/gui/components/tab_hover.png"
    );
    public static final int PANEL_WIDTH = 32;
    public static final int PANEL_HEIGHT = 32;
    public static final int PANEL_X_OFFSET = 6;
    public static final int PANEL_Y_OFFSET = 4;


    protected final ResourceLocation type;
    /**
     * @return the identifier of the tabs type that produced this tabs instance.
     */
    public ResourceLocation type() {
        return this.type;
    }

    public InteractionTab(ResourceLocation type, RegistryFriendlyByteBuf buffer) {
        this.type = type;
    }
    public InteractionTab(ResourceLocation type) {
        this.type = type;
    }

    public void writeToStream(RegistryFriendlyByteBuf buffer) {
        buffer.writeResourceLocation(this.type);
    }


    /**
     * @return the display name shown on the tabs button.
     */
    public abstract Component title();

    /**
     * Renders the tab button.
     */
    public void renderTab(GuiGraphics guiGraphics, Font font, Component text,
                          int x, int y, int mouseX, int mouseY, boolean selected) {
        ResourceLocation texture;
        if (mouseX >= x && mouseX <= x + TAB_WIDTH && mouseY >= y && mouseY <= y + TAB_HEIGHT) {
            texture = TAB_HOVER_TEXTURE;
        }
        else if (selected) {
            texture = TAB_SELECTED_TEXTURE;
        } else {
            texture = TAB_TEXTURE;
        }

        RenderSystem.setShaderTexture(0, texture);
        guiGraphics.blit(texture, x, y, 0, 0, TAB_WIDTH, TAB_HEIGHT, TAB_WIDTH, TAB_HEIGHT);
        guiGraphics.drawString(font, text, x + 8, y + 5, 0xFFFFFF, false);
    }

    /**
     * Renders the tabs contents.
     */
    public abstract void renderPanel(GuiGraphics guiGraphics, int xPos, int yPos, int mouseX, int mouseY);
}
