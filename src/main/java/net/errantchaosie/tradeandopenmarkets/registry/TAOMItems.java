package net.errantchaosie.tradeandopenmarkets.registry;

import net.errantchaosie.tradeandopenmarkets.TradeAndOpenMarketsMod;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class TAOMItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TradeAndOpenMarketsMod.MOD_ID);

    public static final DeferredItem<Item> COIN_ITEM = ITEMS.register("coin",
            () -> new Item(new Item.Properties()));


    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
