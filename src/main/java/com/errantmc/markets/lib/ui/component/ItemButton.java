package com.errantmc.markets.lib.ui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class ItemButton extends Button {
    public static final Logger LOG = LogUtils.getLogger();
    public enum ItemAnchor {
        LEFT,
        TOP,
        RIGHT,
        BOTTOM,
        CENTER
    }
    private final Supplier<ItemStack> icon;
    private float xScale = 1;
    private float yScale = 1;
    private float zScale = 1;
    private int imageX = 0;
    private int imageY = 0;
    private int textX = 0;
    private int textY = 0;
    private ItemAnchor textAnchor = ItemAnchor.CENTER;

    public ItemButton(int x, int y, int w, int h, Supplier<ItemStack> icon, OnPress onPress, Component narration) {
        super(x, y, w, h, narration, onPress, DEFAULT_NARRATION);
        this.icon = icon;
        this.setImageOffset(ItemAnchor.CENTER, 0, 0);
    }

    public void setScale(float x, float y) {
        this.xScale = x;
        this.yScale = y;
    }
    public void setScale(float x, float y, float z) {
        this.xScale = x;
        this.yScale = y;
        this.zScale = z;
    }

    public void setImageOffset(ItemAnchor anchor, int x, int y) {
        switch (anchor) {
            case LEFT:
                // center the item in this button
                this.imageX = x + getX();
                this.imageY = y + getY() + (this.getHeight() - 16) / 2;
                break;
            case RIGHT:
                // center the item in this button
                this.imageX = x + getX() + (this.getWidth()  - 16);
                this.imageY = y + getY() + (this.getHeight() - 16) / 2;
                break;
            case TOP:
                // center the item in this button
                this.imageX = x + getX() + (this.getWidth()  - 16) / 2;
                this.imageY = y + getY();
                break;
            case BOTTOM:
                // center the item in this button
                this.imageX = x + getX() + (this.getWidth()  - 16) / 2;
                this.imageY = y + getY() + (this.getHeight() - 16);
                break;
            case CENTER:
            default:
                // center the item in this button
                this.imageX = x + getX() + (this.getWidth()  - 16) / 2;
                this.imageY = y + getY() + (this.getHeight() - 16) / 2;
                break;
        }
    }

    public void setTextOffset(ItemAnchor anchor, int x, int y) {
        this.textAnchor = anchor;
        switch (textAnchor) {
            case LEFT:
                this.textX = x + getX() + 2;
                this.textY = y + getY() + this.getHeight() / 2;
                break;
            case RIGHT:
                this.textX = x + getX() - 2 + this.getWidth();
                this.textY = y + getY() + this.getHeight() / 2;
                break;
            case TOP:
                this.textX = x + getX() + this.getWidth() / 2;
                this.textY = y + getY() + 2;
                break;
            case BOTTOM:
                this.textX = x + getX() + this.getWidth() / 2;
                this.textY = y + getY() - 2 + this.getHeight();
                break;
            case CENTER:
            default:
                this.textX = x + getX() + this.getWidth() / 2;
                this.textY = y + getY() + this.getHeight() / 2;
                break;
        }
    }



    public ItemStack getIconItem()
    {
        return icon.get();
    }



    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        // draw vanilla button background (hover/disabled included)
        Minecraft minecraft = Minecraft.getInstance();
        gg.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        gg.blitSprite(SPRITES.get(this.active, this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight());
        gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        // draw text
        this.renderMessageText(gg, minecraft);

        // draw image
        var p = gg.pose();
        ItemStack stack = icon.get();
        if (!stack.isEmpty()) {
            p.pushPose();
            //p.scale(xScale, yScale, zScale);
            gg.renderItem(stack, imageX, imageY);
            p.popPose();
            // optional: overlay text or grayed-out tint when !active
            if (!this.active) {
                p.pushPose();
                p.translate(0, 0, 200);
                gg.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x60_000000); // darken
                p.popPose();
            }
        }
    }

    private void renderMessageText(GuiGraphics gg, Minecraft minecraft) {
        var color = this.getFGColor() | Mth.ceil(this.alpha * 255.0F) << 24;
        var message = this.getMessage();
        switch (textAnchor) {
            case LEFT:
                gg.drawString(minecraft.font, message, textX, textY, color);
                break;
            case RIGHT:
                gg.drawString(minecraft.font, message,
                        textX - minecraft.font.width(message),
                        textY - minecraft.font.lineHeight/2, color);
                break;
            default:
                gg.drawCenteredString(minecraft.font, message, textX, textY- minecraft.font.lineHeight, color);
                break;
        }

    }

}
