package net.errantchaosie.tradeandopenmarkets;

import net.errantchaosie.tradeandopenmarkets.block.TAOMBlocks;
import net.errantchaosie.tradeandopenmarkets.block.entity.TAOMBlockEntities;
import net.errantchaosie.tradeandopenmarkets.block.entity.eventhandlers.CoinBlockLeftClickHandler;
import net.errantchaosie.tradeandopenmarkets.component.TAOMComponents;
import net.errantchaosie.tradeandopenmarkets.item.TAOMItems;
import net.errantchaosie.tradeandopenmarkets.item.wallet.WalletAutoDeposit;
import net.errantchaosie.tradeandopenmarkets.registry.*;
import net.errantchaosie.tradeandopenmarkets.screen.TAOMMenus;
import net.errantchaosie.tradeandopenmarkets.villager.TAOMVillagers;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(TradeAndOpenMarketsMod.MOD_ID)
public class TradeAndOpenMarketsMod {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "tradeandopenmarkets";

    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public TradeAndOpenMarketsMod(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        //NeoForge.EVENT_BUS.register(this);

        TAOMComponents.register(modEventBus);
        TAOMCreativeModeTabs.register(modEventBus);
        TAOMItems.register(modEventBus);
        TAOMBlocks.register(modEventBus);
        TAOMBlockEntities.register(modEventBus);
        TAOMVillagers.register(modEventBus);
        TAOMSounds.register(modEventBus);
        TAOMMenus.register(modEventBus);
        // Register the item to a creative tab
        //modEventBus.addListener(this::addCreative);
        //modEventBus.addListener(this::clientSetup);
        // Gameplay / world events go on the global NeoForge bus:
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(CoinBlockLeftClickHandler::onLeftClickBlock);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(new WalletAutoDeposit());
        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
    }



}
