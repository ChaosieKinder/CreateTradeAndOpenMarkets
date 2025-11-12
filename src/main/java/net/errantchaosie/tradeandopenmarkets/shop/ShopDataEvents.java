package net.errantchaosie.tradeandopenmarkets.shop;

import net.errantchaosie.tradeandopenmarkets.TradeAndOpenMarketsMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

@Mod.EventBusSubscriber(modid = TradeAndOpenMarketsMod.MOD_ID)
public final class ShopDataEvents {
    private ShopDataEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(ShopDefinitionLoader.getInstance());
    }
}
