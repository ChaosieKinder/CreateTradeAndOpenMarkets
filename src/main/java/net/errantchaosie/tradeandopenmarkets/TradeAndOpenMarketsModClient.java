package net.errantchaosie.tradeandopenmarkets;

import com.mojang.logging.LogUtils;
import net.errantchaosie.tradeandopenmarkets.block.entity.TAOMBlockEntities;
import net.errantchaosie.tradeandopenmarkets.block.entity.renderers.CoinBlockEntityRenderer;
import net.errantchaosie.tradeandopenmarkets.item.CoinStackItem;
import net.errantchaosie.tradeandopenmarkets.item.TAOMItems;
import net.errantchaosie.tradeandopenmarkets.item.idcard.IdentityCardItemRenderer;
import net.errantchaosie.tradeandopenmarkets.item.idcard.IdentityCardTooltip;
import net.errantchaosie.tradeandopenmarkets.item.idcard.IdentityCardTooltipClient;
import net.errantchaosie.tradeandopenmarkets.item.idcard.client.IdentityCardClientExtensions;
import net.errantchaosie.tradeandopenmarkets.item.wallet.WalletTooltip;
import net.errantchaosie.tradeandopenmarkets.item.wallet.WalletTooltipClient;
import net.errantchaosie.tradeandopenmarkets.screen.TAOMMenus;
import net.errantchaosie.tradeandopenmarkets.screen.safe.SafeScreen;
import net.errantchaosie.tradeandopenmarkets.screen.wallet.WalletScreen;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = TradeAndOpenMarketsMod.MOD_ID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = TradeAndOpenMarketsMod.MOD_ID, value = Dist.CLIENT)
public class TradeAndOpenMarketsModClient {
    public static final Logger LOG = LogUtils.getLogger();
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
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        LOG.info(TradeAndOpenMarketsMod.MOD_ID + " Registering ClientExtensions");
        event.registerItem(new IdentityCardClientExtensions(), TAOMItems.ID_CARD.get());
    }

    @SubscribeEvent
    public static void onRegisterTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(WalletTooltip.class, WalletTooltipClient::new);
        event.register(IdentityCardTooltip.class, IdentityCardTooltipClient::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(TAOMMenus.SAFE_MENU.get(), SafeScreen::new);
        event.register(TAOMMenus.WALLET_MENU.get(), WalletScreen::new);
    }


    @SubscribeEvent
    private static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(IdentityCardItemRenderer.CARD_MODEL_LOCATION);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(TAOMItems.COIN_STACK_ITEM.get(),
                    ResourceLocation.fromNamespaceAndPath(TradeAndOpenMarketsMod.MOD_ID, "stack_tier"),
                    (stack, level, entity, seed) -> CoinStackItem.tier(stack));
        });
    }

}
