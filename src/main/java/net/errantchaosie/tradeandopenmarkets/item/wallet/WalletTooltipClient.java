package net.errantchaosie.tradeandopenmarkets.item.wallet;

import net.errantchaosie.tradeandopenmarkets.item.wallet.WalletTooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/**
 * Client rendering logic for {@link WalletTooltip}.
 */
@OnlyIn(Dist.CLIENT)
public class WalletTooltipClient implements ClientTooltipComponent {
    private static final int SLOT_SIZE = 18;
    private static final int BORDER = 3;
    private static final int MAX_COLUMNS = 3;

    private final List<ItemStack> items;

    public WalletTooltipClient(WalletTooltip tooltip) {
        this.items = tooltip.items();
    }

    @Override
    public int getHeight() {
        int rows = Math.max(1, (items.size() + MAX_COLUMNS - 1) / MAX_COLUMNS);
        return rows * SLOT_SIZE + BORDER * 2;
    }

    @Override
    public int getWidth(Font font) {
        int columns = Math.min(MAX_COLUMNS, Math.max(1, items.size()));
        return columns * SLOT_SIZE + BORDER * 2;
    }

    @Override
    public void renderImage(Font font, int mouseX, int mouseY, GuiGraphics guiGraphics) {
        for (int index = 0; index < items.size(); index++) {
            ItemStack stack = items.get(index);
            if (stack.isEmpty()) {
                continue;
            }

            int col = index % MAX_COLUMNS;
            int row = index / MAX_COLUMNS;
            int x = mouseX + BORDER + col * SLOT_SIZE;
            int y = mouseY + BORDER + row * SLOT_SIZE;

            guiGraphics.renderItem(stack, x, y);
            guiGraphics.renderItemDecorations(font, stack, x, y);
        }
    }
}