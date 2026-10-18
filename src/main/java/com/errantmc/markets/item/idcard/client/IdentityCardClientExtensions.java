package com.errantmc.markets.item.idcard.client;

import com.errantmc.markets.item.idcard.IdentityCardItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class IdentityCardClientExtensions implements IClientItemExtensions {
    private final IdentityCardItemRenderer renderer = new IdentityCardItemRenderer();

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return renderer;
    }
}