package net.errantchaosie.tradeandopenmarkets.item;

import net.errantchaosie.tradeandopenmarkets.TradeAndOpenMarketsMod;
import net.errantchaosie.tradeandopenmarkets.data.CoinDenomination;
import net.errantchaosie.tradeandopenmarkets.item.wallet.WalletItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class TAOMItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TradeAndOpenMarketsMod.MOD_ID);

    public static final DeferredItem<Item> COIN_COPPER   = ITEMS.register("coin_copper",
            () -> new CoinItem(new Item.Properties(), CoinDenomination.COPPER));

    public static final DeferredItem<Item> COIN_BRONZE   = ITEMS.register("coin_bronze",
            () -> new CoinItem(new Item.Properties(), CoinDenomination.BRONZE));

    public static final DeferredItem<Item> COIN_SILVER   = ITEMS.register("coin_silver",
            () -> new CoinItem(new Item.Properties(), CoinDenomination.SILVER));

    public static final DeferredItem<Item> COIN_GOLD = ITEMS.register("coin_gold",
            () -> new CoinItem(new Item.Properties(), CoinDenomination.GOLD));

    public static final DeferredItem<Item> COIN_ELECTRUM = ITEMS.register("coin_electrum",
            () -> new CoinItem(new Item.Properties(), CoinDenomination.ELECTRUM));

    public static final DeferredItem<Item> COIN_PLATINUM = ITEMS.register("coin_platinum",
            () -> new CoinItem(new Item.Properties(), CoinDenomination.PLATINUM));

    public static final DeferredItem<Item> COIN_EMERALD  = ITEMS.register("coin_emerald",
            () -> new CoinItem(new Item.Properties(), CoinDenomination.EMERALD));

    public static final DeferredItem<Item> WALLET_LEATHER_ITEM  = ITEMS.register("wallet_leather",
            () -> new WalletItem(new Item.Properties()));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
