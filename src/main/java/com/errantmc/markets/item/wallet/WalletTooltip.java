package com.errantmc.markets.item.wallet;

import com.errantmc.markets.item.wallet.WalletTooltipClient;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tooltip payload for displaying wallet contents in the hover tooltip.
 */
public final class WalletTooltip implements TooltipComponent {
    private final List<ItemStack> items;

    public WalletTooltip(List<ItemStack> stacks) {
        List<ItemStack> copy = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            copy.add(stack.copy());
        }
        this.items = Collections.unmodifiableList(copy);
    }

    public List<ItemStack> items() {
        return this.items;
    }

    @OnlyIn(Dist.CLIENT)
    public net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent createClientTooltipComponent() {
        return new WalletTooltipClient(this);
    }
}