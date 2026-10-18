package com.errantmc.markets.npcinteractions.components;

import com.errantmc.markets.ErrantTrade;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
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

    // ---------- the grid ----------
    // A 3x9 grid of cells, one entry per cell, drawn over the panel art. Drawing, hit-testing and
    // tooltips share this geometry, so what is drawn and what is clicked cannot drift apart.

    private static final ResourceLocation GRID_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ErrantTrade.MOD_ID, "textures/gui/npcinteractions/tab_sell.png");
    /** The panel art, drawn into shop_base's brown area. */
    public static final int PANEL_X = 17, PANEL_Y = 31, PANEL_TEXTURE_WIDTH = 242, PANEL_TEXTURE_HEIGHT = 80;
    /** The first cell's item position, in screen space relative to the screen's corner. */
    private static final int GRID_X = PANEL_X + 44, GRID_Y = PANEL_Y + 9, CELL = 18;
    public static final int COLUMNS = 9, ROWS = 3, PER_PAGE = COLUMNS * ROWS;

    public int pageCount() {
        return Math.max(1, (this.entries.size() + PER_PAGE - 1) / PER_PAGE);
    }

    /** The index of the entry under the mouse on {@code page}, or -1. */
    public int entryAt(int left, int top, double mouseX, double mouseY, int page) {
        int col = (int) Math.floor((mouseX - (left + GRID_X - 1)) / CELL);
        int row = (int) Math.floor((mouseY - (top + GRID_Y - 1)) / CELL);
        if (col < 0 || col >= COLUMNS || row < 0 || row >= ROWS) return -1;
        int index = page * PER_PAGE + row * COLUMNS + col;
        return index < this.entries.size() ? index : -1;
    }

    /** Draws the panel art and {@code page}'s entries. */
    public void renderGrid(GuiGraphics guiGraphics, Font font, int left, int top, int mouseX, int mouseY, int page) {
        guiGraphics.blit(GRID_TEXTURE, left + PANEL_X, top + PANEL_Y, 0, 0,
                PANEL_TEXTURE_WIDTH, PANEL_TEXTURE_HEIGHT, PANEL_TEXTURE_WIDTH, PANEL_TEXTURE_HEIGHT);
        int hovered = entryAt(left, top, mouseX, mouseY, page);
        int first = page * PER_PAGE;
        for (int i = first; i < Math.min(this.entries.size(), first + PER_PAGE); i++) {
            ShopEntry entry = this.entries.get(i);
            int x = left + GRID_X + ((i - first) % COLUMNS) * CELL;
            int y = top + GRID_Y + ((i - first) / COLUMNS) * CELL;
            guiGraphics.renderItem(entry.item(), x, y);
            guiGraphics.renderItemDecorations(font, entry.item(), x, y);
            if (!entry.inStock()) {
                guiGraphics.fill(x, y, x + 16, y + 16, 200, 0xA0303030); // greyed out, over the item
            }
            if (i == hovered) {
                guiGraphics.fill(x, y, x + 16, y + 16, 200, 0x80FFFFFF); // as a hovered slot
            }
        }
    }

    /**
     * The entry's own item tooltip, then what the trade costs or pays, its stock, anything else it
     * needs, and whether the player can do it right now.
     */
    public List<Component> tooltip(ShopEntry entry, List<Component> itemLines, TradeView view) {
        List<Component> lines = new ArrayList<>(itemLines);
        lines.add(Component.empty());
        lines.add(Component.translatable(priceKey(), MoneyFormat.format(entry.price())).withStyle(ChatFormatting.GOLD));
        if (!entry.extraCost().isEmpty()) {
            lines.add(Component.translatable("screen.erranttrade.npcinteractions.also_needs",
                    entry.extraCost().getCount(), entry.extraCost().getHoverName()).withStyle(ChatFormatting.GRAY));
        }
        if (!entry.inStock()) {
            lines.add(Component.translatable("screen.erranttrade.npcinteractions.out_of_stock").withStyle(ChatFormatting.RED));
        } else if (entry.hasLimitedStock()) {
            lines.add(Component.translatable("screen.erranttrade.npcinteractions.stock", entry.stock()).withStyle(ChatFormatting.GRAY));
        }
        Component status = status(entry, view);
        if (status != null) lines.add(status);
        return lines;
    }

    /** Can the player make this trade now? The server decides; this only says what it will say. */
    protected abstract Component status(ShopEntry entry, TradeView view);

    /** The translation key of the price line: what the player pays, or what they are paid. */
    protected abstract String priceKey();

    /**
     * What a tooltip may know about the player: the money they carry, and how many of an item.
     * Supplied by the client screen.
     */
    public interface TradeView {
        long money();

        int count(ItemStack item);
    }

    /** Custom tab types draw themselves; shop tabs are drawn by the screen through renderGrid. */
    @Override
    public void renderPanel(GuiGraphics guiGraphics, int xPos, int yPos, int mouseX, int mouseY) {
        renderGrid(guiGraphics, net.minecraft.client.Minecraft.getInstance().font, xPos, yPos, mouseX, mouseY, 0);
    }
}
