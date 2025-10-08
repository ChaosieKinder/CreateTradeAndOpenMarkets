package net.errantchaosie.tradeandopenmarkets.item;

import net.errantchaosie.tradeandopenmarkets.TradeAndOpenMarketsMod;
import net.errantchaosie.tradeandopenmarkets.item.coins.*;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class TAOMItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TradeAndOpenMarketsMod.MOD_ID);

    public static final DeferredItem<Item> COIN_COPPER = ITEMS.register("coin_copper",
            () -> new CoinCopper(new Item.Properties()));

    public static final DeferredItem<Item> COIN_BRONZE = ITEMS.register("coin_bronze",
            () -> new CoinBronze(new Item.Properties()));

    public static final DeferredItem<Item> COIN_SILVER = ITEMS.register("coin_silver",
            () -> new CoinSilver(new Item.Properties()));

    public static final DeferredItem<Item> COIN_ELECTRUM = ITEMS.register("coin_electrum",
            () -> new CoinElectrum(new Item.Properties()));

    public static final DeferredItem<Item> COIN_EMERALD = ITEMS.register("coin_emerald",
            () -> new CoinEmerald(new Item.Properties()));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
