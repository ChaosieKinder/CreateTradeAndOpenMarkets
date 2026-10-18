package com.errantmc.markets;

import com.mojang.logging.LogUtils;
import com.errantmc.markets.block.entity.TradeBlockEntities;
import com.errantmc.markets.block.entity.renderers.CoinBlockEntityRenderer;
import com.errantmc.markets.item.CoinStackItem;
import com.errantmc.markets.item.TradeItems;
import com.errantmc.markets.item.idcard.IdentityCardItemRenderer;
import com.errantmc.markets.item.idcard.IdentityCardTooltip;
import com.errantmc.markets.item.idcard.IdentityCardTooltipClient;
import com.errantmc.markets.item.idcard.client.IdentityCardClientExtensions;
import com.errantmc.markets.item.wallet.WalletTooltip;
import com.errantmc.markets.item.wallet.WalletTooltipClient;
import com.errantmc.markets.screen.TradeMenus;
import com.errantmc.markets.screen.safe.SafeScreen;
import com.errantmc.markets.screen.villager.VillagerInteractionScreen;
import com.errantmc.markets.screen.wallet.WalletScreen;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = ErrantTrade.MOD_ID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = ErrantTrade.MOD_ID, value = Dist.CLIENT)
public class ErrantTradeClient {
    public static final Logger LOG = LogUtils.getLogger();

    public ErrantTradeClient(IEventBus modEventBus, ModContainer container) {

        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                TradeBlockEntities.COIN_PILE.get(),
                CoinBlockEntityRenderer::new
        );
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        LOG.info(ErrantTrade.MOD_ID + " Registering ClientExtensions");
        event.registerItem(new IdentityCardClientExtensions(), TradeItems.ID_CARD.get());
    }

    @SubscribeEvent
    public static void onRegisterTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(WalletTooltip.class, WalletTooltipClient::new);
        event.register(IdentityCardTooltip.class, IdentityCardTooltipClient::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(TradeMenus.SAFE_MENU.get(), SafeScreen::new);
        event.register(TradeMenus.WALLET_MENU.get(), WalletScreen::new);
        event.register(TradeMenus.VILLAGER_INTERACTION_MENU.get(), VillagerInteractionScreen::new);
    }


    @SubscribeEvent
    private static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(IdentityCardItemRenderer.CARD_MODEL_LOCATION);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(TradeItems.COIN_STACK_ITEM.get(),
                    ResourceLocation.fromNamespaceAndPath(ErrantTrade.MOD_ID, "stack_tier"),
                    (stack, level, entity, seed) -> CoinStackItem.tier(stack));
        });
    }


}
