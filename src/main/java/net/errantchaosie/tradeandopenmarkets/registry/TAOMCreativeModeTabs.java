package net.errantchaosie.tradeandopenmarkets.registry;

import net.errantchaosie.tradeandopenmarkets.TradeAndOpenMarketsMod;
import net.errantchaosie.tradeandopenmarkets.block.SafeBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class TAOMCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB,  TradeAndOpenMarketsMod.MOD_ID);

    public static final Supplier<CreativeModeTab> TAOM_ITEMS_TAB = CREATIVE_MODE_TAB.register("tradeandopenmarkets_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(TAOMBlocks.SAFE_BLOCK.get().asItem()))
                    .title(Component.translatable("creativetab.tradeandopenmarkets.items_tab"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(TAOMItems.COIN_ITEM);
                        output.accept(TAOMBlocks.SAFE_BLOCK.get());
                    }).build());

    public static void register(IEventBus bus) {
        CREATIVE_MODE_TAB.register(bus);
    }


}
