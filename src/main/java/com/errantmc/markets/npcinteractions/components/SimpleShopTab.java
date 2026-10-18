package com.errantmc.markets.npcinteractions.components;

import com.errantmc.markets.domain.MoneyFormat;
import com.errantmc.markets.npcinteractions.trading.ShopEntry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Basic implementation for item based tabs like buying or selling.
 */
public abstract class SimpleShopTab extends InteractionTab {

    private final Component title;
    private final List<ShopEntry> entries;

    public SimpleShopTab(ResourceLocation type, Component title, List<ShopEntry> entries) {
        super(type);
        this.title = title;
        this.entries = new ArrayList<>(entries);
    }

    public SimpleShopTab(ResourceLocation type, RegistryFriendlyByteBuf buf) {
        super(type, buf);
        this.title = ComponentSerialization.STREAM_CODEC.decode(buf);
        int entryCount = buf.readVarInt();
        this.entries = new ArrayList<>(entryCount);
        for (int i = 0; i < entryCount; i++) {
            this.entries.add(new ShopEntry(buf));
        }
    }

    @Override
    public void writeToStream(RegistryFriendlyByteBuf buffer) {
        super.writeToStream(buffer);
        ComponentSerialization.STREAM_CODEC.encode(buffer, this.title);
        buffer.writeVarInt(this.entries.size());
        for (ShopEntry entry : this.entries) {
            entry.writeToStream(buffer);
        }
    }

    @Override
    public Component title() {
        return this.title;
    }

    public List<ShopEntry> entries() {
        return Collections.unmodifiableList(this.entries);
    }

    protected List<ShopEntry> mutableEntries() {
        return this.entries;
    }

    // Row geometry, shared by drawing and hit-testing so the two cannot drift apart
    private static final int ROW_HEIGHT = 28;
    private static final int ROW_WIDTH = 180;
    private static final int MAX_ROWS = 11;

    private static int rowX(int xPos) {
        return xPos + PANEL_X_OFFSET + 18;
    }

    private static int rowY(int yPos, int row) {
        return yPos + PANEL_Y_OFFSET + 44 + row * ROW_HEIGHT;
    }

    /** The entry under the mouse, or -1. */
    public int entryAt(int xPos, int yPos, double mouseX, double mouseY) {
        int rows = Math.min(this.entries.size(), MAX_ROWS);
        int x = rowX(xPos);
        for (int row = 0; row < rows; row++) {
            int y = rowY(yPos, row);
            if (mouseX >= x && mouseX < x + ROW_WIDTH && mouseY >= y && mouseY < y + ROW_HEIGHT) return row;
        }
        return -1;
    }

    /** The translation key of the price line: what the player pays, or what they are paid. */
    protected abstract String priceKey();

    @Override
    public void renderPanel(GuiGraphics guiGraphics, int xPos, int yPos, int mouseX, int mouseY) {
        Font font = net.minecraft.client.Minecraft.getInstance().font;
        int x = rowX(xPos);
        int rows = Math.min(this.entries.size(), MAX_ROWS);
        for (int row = 0; row < rows; row++) {
            ShopEntry entry = this.entries.get(row);
            int y = rowY(yPos, row);
            if (entryAt(xPos, yPos, mouseX, mouseY) == row) {
                guiGraphics.fill(x - 2, y - 2, x + ROW_WIDTH, y + ROW_HEIGHT - 4, 0x22FFFFFF);
            }
            guiGraphics.renderItem(entry.item(), x, y);
            guiGraphics.renderItemDecorations(font, entry.item(), x, y);
            int textX = x + 20;
            int colour = entry.inStock() ? 0x202020 : 0x903030;
            guiGraphics.drawString(font, entry.item().getHoverName(), textX, y + 4, 0x404040, false);
            Component price = Component.translatable(priceKey(), MoneyFormat.format(entry.price()));
            guiGraphics.drawString(font, price, textX, y + 16, colour, false);
            Component stock = entry.hasLimitedStock()
                    ? Component.translatable("screen.erranttrade.npcinteractions.stock", entry.stock())
                    : Component.translatable("screen.erranttrade.npcinteractions.stock_unlimited");
            guiGraphics.drawString(font, stock, textX + 80, y + 16, colour, false);
            if (!entry.extraCost().isEmpty()) {
                // A vanilla trade's second cost, such as the librarian's book
                int extraX = textX + 80 + font.width(stock) + 6;
                guiGraphics.drawString(font, "+", extraX, y + 16, colour, false);
                guiGraphics.renderItem(entry.extraCost(), extraX + 8, y + 10);
                guiGraphics.renderItemDecorations(font, entry.extraCost(), extraX + 8, y + 10);
            }
        }
    }
}
