package com.errantmc.markets.item.idcard;

import com.errantmc.markets.component.IdentityCardData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public final class IdentityCardTooltip implements TooltipComponent {
    private final IdentityCardData data;
    private final Component ownerLine;
    private final Component createdLine;

    public IdentityCardTooltip(IdentityCardData data, Component ownerLine, Component createdLine) {
        this.data = data;
        this.ownerLine = ownerLine;
        this.createdLine = createdLine;
    }

    public IdentityCardData data() {
        return data;
    }

    public Component ownerLine() {
        return ownerLine;
    }

    public Component createdLine() {
        return createdLine;
    }

    @OnlyIn(Dist.CLIENT)
    public net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent createClientTooltipComponent() {
        return new IdentityCardTooltipClient(this);
    }
}