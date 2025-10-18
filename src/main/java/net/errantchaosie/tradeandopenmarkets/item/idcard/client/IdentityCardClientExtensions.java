package net.errantchaosie.tradeandopenmarkets.item.idcard.client;

import net.errantchaosie.tradeandopenmarkets.item.idcard.IdentityCardItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class IdentityCardClientExtensions implements IClientItemExtensions {
    private final IdentityCardItemRenderer renderer = new IdentityCardItemRenderer();

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return renderer;
    }
}