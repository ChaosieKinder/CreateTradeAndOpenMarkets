package net.errantchaosie.tradeandopenmarkets.shop;

import net.errantchaosie.tradeandopenmarkets.TradeAndOpenMarketsMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

@EventBusSubscriber(modid = TradeAndOpenMarketsMod.MOD_ID)
public final class ShopDataEvents {
    private ShopDataEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(ShopDefinitionLoader.getInstance());
    }
}
