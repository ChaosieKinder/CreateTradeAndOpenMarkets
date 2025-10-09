package net.errantchaosie.tradeandopenmarkets.item;

import net.errantchaosie.tradeandopenmarkets.TradeAndOpenMarketsMod;
import net.errantchaosie.tradeandopenmarkets.item.coins.*;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class TAOMItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TradeAndOpenMarketsMod.MOD_ID);

    public static final DeferredItem<Item> COIN_COPPER   = ITEMS.register("coin_copper",
            () -> new CoinItemBase(new Item.Properties(), CoinItemBase.Denomination.COPPER));

    public static final DeferredItem<Item> COIN_BRONZE   = ITEMS.register("coin_bronze",
            () -> new CoinItemBase(new Item.Properties(), CoinItemBase.Denomination.BRONZE));

    public static final DeferredItem<Item> COIN_SILVER   = ITEMS.register("coin_silver",
            () -> new CoinItemBase(new Item.Properties(), CoinItemBase.Denomination.SILVER));

    public static final DeferredItem<Item> COIN_ELECTRUM = ITEMS.register("coin_electrum",
            () -> new CoinItemBase(new Item.Properties(), CoinItemBase.Denomination.ELECTRUM));

    public static final DeferredItem<Item> COIN_EMERALD  = ITEMS.register("coin_emerald",
            () -> new CoinItemBase(new Item.Properties(), CoinItemBase.Denomination.EMERALD));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
