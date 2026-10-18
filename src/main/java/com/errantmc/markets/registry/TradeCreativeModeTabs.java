package com.errantmc.markets.registry;

import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.block.TradeBlocks;
import com.errantmc.markets.item.TradeItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class TradeCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB,  ErrantTrade.MOD_ID);

    public static final Supplier<CreativeModeTab> ITEMS_TAB = CREATIVE_MODE_TAB.register("erranttrade_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(TradeBlocks.SAFE_BLOCK.get().asItem()))
                    .title(Component.translatable("creativetab.erranttrade.items_tab"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(TradeItems.COIN_COPPER_BIT.get());
                        output.accept(TradeItems.COIN_COPPER_SHARD.get());
                        output.accept(TradeItems.COIN_SILVER_BIT.get());
                        output.accept(TradeItems.COIN_SILVER_CROWN.get());
                        output.accept(TradeItems.COIN_GOLD_MARK.get());
                        output.accept(TradeItems.COIN_GOLD_REGAL.get());
                        output.accept(TradeItems.COIN_EMERALD_SUN.get());
                        output.accept(TradeItems.COIN_EMERALD_THRONE.get());
                        output.accept(TradeItems.WALLET_LEATHER_ITEM.get());
                        output.accept(TradeItems.ID_CARD.get());
                        output.accept(TradeBlocks.SAFE_BLOCK.get());
                    }).build());

    public static void register(IEventBus bus) {
        CREATIVE_MODE_TAB.register(bus);
    }


}
