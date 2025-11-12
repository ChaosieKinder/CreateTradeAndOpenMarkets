package net.errantchaosie.tradeandopenmarkets.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.errantchaosie.tradeandopenmarkets.TradeAndOpenMarketsMod;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class InventoryHelper {

    // CREDIT GOES TO: diesieben07 | https://github.com/diesieben07/SevenCommons
    // must assign a slot number to each of the slots used by the GUI.
    // For this container, we can see both the tile inventory's slots as well as the player inventory slots and the hotbar.
    // Each time we add a Slot to the container, it automatically increases the slotIndex, which means
    //  0 - 8 = hotbar slots (which will map to the InventoryPlayer slot numbers 0 - 8)
    //  9 - 35 = player inventory slots (which map to the InventoryPlayer slot numbers 9 - 35)
    //  36 - 44 = TileInventory slots, which map to our TileEntity slot numbers 0 - 8)
    public static final int HOTBAR_SLOT_COUNT = 9;
    public static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    public static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    public static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    public static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    public static final int VANILLA_FIRST_SLOT_INDEX = 0;
    public static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;


    private static final ResourceLocation BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            TradeAndOpenMarketsMod.MOD_ID,
            "textures/gui/components/button.png"
    );
    private static final ResourceLocation BUTTON_HOVER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            TradeAndOpenMarketsMod.MOD_ID,
            "textures/gui/components/button_hover.png"
    );
    private static final int TAB_WIDTH = 38;
    private static final int TAB_HEIGHT = 17;
    private static final ResourceLocation TAB_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            TradeAndOpenMarketsMod.MOD_ID,
            "textures/gui/components/tab_deselected.png"
    );
    private static final ResourceLocation TAB_SELECTED_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            TradeAndOpenMarketsMod.MOD_ID,
            "textures/gui/components/tab_selected.png"
    );
    private static final ResourceLocation TAB_HOVER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            TradeAndOpenMarketsMod.MOD_ID,
            "textures/gui/components/tab_hover.png"
    );

    public static void drawTab(GuiGraphics guiGraphics, Font font, Component text,
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
}
