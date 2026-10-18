package com.errantmc.markets.lib.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class GuiHelpers {

    private static void drawScaled(GuiGraphics gg, Font font, String text, float x, float y, float scale, int color) {
        var pose = gg.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1f);
        gg.drawString(font, text, 0, 0, color, false);
        pose.popPose();
    }

    private static void drawScaled(GuiGraphics gg, String text, float x, float y, float scale, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        drawScaled(gg, minecraft.font, text, x, y, scale, color);
    }
}
