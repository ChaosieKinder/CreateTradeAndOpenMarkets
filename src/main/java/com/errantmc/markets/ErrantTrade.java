package com.errantmc.markets;

import com.errantmc.markets.block.TradeBlocks;
import com.errantmc.markets.block.entity.TradeBlockEntities;
import com.errantmc.markets.block.entity.eventhandlers.CoinBlockLeftClickHandler;
import com.errantmc.markets.component.TradeComponents;
import com.errantmc.markets.item.TradeItems;
import com.errantmc.markets.item.idcard.IdentityCardDataSyncHandler;
import com.errantmc.markets.item.wallet.WalletAutoDeposit;
import com.errantmc.markets.network.TradeNetwork;
import com.errantmc.markets.registry.*;
import com.errantmc.markets.screen.TradeMenus;
import com.errantmc.markets.villager.TradeVillagers;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(ErrantTrade.MOD_ID)
public class ErrantTrade {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "erranttrade";

    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public ErrantTrade(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(TradeNetwork::register);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        //NeoForge.EVENT_BUS.register(this);

        TradeComponents.register(modEventBus);
        TradeCreativeModeTabs.register(modEventBus);
        TradeItems.register(modEventBus);
        TradeBlocks.register(modEventBus);
        TradeBlockEntities.register(modEventBus);
        TradeVillagers.register(modEventBus);
        TradeSounds.register(modEventBus);
        TradeMenus.register(modEventBus);
        // Register the item to a creative tab
        //modEventBus.addListener(this::addCreative);
        //modEventBus.addListener(this::clientSetup);
        // Gameplay / world events go on the global NeoForge bus:
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(CoinBlockLeftClickHandler::onLeftClickBlock);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(new WalletAutoDeposit());
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(new IdentityCardDataSyncHandler());
        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
    }



}
