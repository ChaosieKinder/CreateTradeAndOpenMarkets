package com.errantmc.markets.npcinteractions.tabs.buy;

import com.errantmc.markets.npcinteractions.components.SimpleShopTab;
import com.errantmc.markets.npcinteractions.trading.ShopEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Simple extension that exposes helper lookups for buy tabs so other tabs
 * types (like sells) can interact with their inventory data.
 */
public class BuyShopTab extends SimpleShopTab {
    public BuyShopTab(ResourceLocation type, Component title, List<ShopEntry> entries) {
        super(type, title, entries);
    }

    public BuyShopTab(ResourceLocation type, RegistryFriendlyByteBuf buf) {
        super(type, buf);
    }

    public Optional<ShopEntry> findEntry(ItemStack stack) {
        for (ShopEntry entry : entries()) {
            if (ItemStack.isSameItemSameComponents(entry.item(), stack)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    @Override
    public void writeToStream(RegistryFriendlyByteBuf buffer) {
        super.writeToStream(buffer);
    }

    @Override
    public void renderPanel(GuiGraphics guiGraphics, int xPos, int yPos, int mouseX, int mouseY) {
        int x = xPos + PANEL_X_OFFSET + 18;
        int y = yPos + PANEL_Y_OFFSET + 44;
        int index = 0;
        /* Render up to 12 entries */
        for (ShopEntry entry : entries()) {
            if (index >= 11) {
                break;
            }
            var font = Minecraft.getInstance().font;
            guiGraphics.renderItem(entry.item(), x, y);
            guiGraphics.renderItemDecorations(font, entry.item(), x, y);
            Component priceText = Component.literal("Price: " + entry.price());
            Component stockText = entry.hasLimitedStock()
                    ? Component.literal("Stock: " + entry.stock())
                    : Component.literal("Stock: ∞");
            int textX = x + 20;
            guiGraphics.drawString(font, entry.item().getHoverName(), textX, y + 4, 0x404040, false);
            guiGraphics.drawString(font, priceText, textX, y + 16, 0x202020, false);
            guiGraphics.drawString(font, stockText, textX + 70, y + 16, 0x202020, false);
            y += 28;
            index++;
        }
    }
}
