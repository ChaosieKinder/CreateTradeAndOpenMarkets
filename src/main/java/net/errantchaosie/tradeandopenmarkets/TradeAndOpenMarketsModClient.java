package net.errantchaosie.tradeandopenmarkets;

import net.errantchaosie.tradeandopenmarkets.block.entity.TAOMBlockEntities;
import net.errantchaosie.tradeandopenmarkets.block.entity.renderers.CoinBlockEntityRenderer;
import net.errantchaosie.tradeandopenmarkets.screen.TAOMMenus;
import net.errantchaosie.tradeandopenmarkets.screen.safe.SafeScreen;
import net.errantchaosie.tradeandopenmarkets.screen.wallet.WalletScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = TradeAndOpenMarketsMod.MOD_ID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = TradeAndOpenMarketsMod.MOD_ID, value = Dist.CLIENT)
public class TradeAndOpenMarketsModClient {
    public TradeAndOpenMarketsModClient(IEventBus modEventBus, ModContainer container) {

        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                TAOMBlockEntities.COIN_BLOCKENTITY.get(),
                CoinBlockEntityRenderer::new
        );
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(TAOMMenus.SAFE_MENU.get(), SafeScreen::new);
        event.register(TAOMMenus.WALLET_MENU.get(), WalletScreen::new);
    }

}
