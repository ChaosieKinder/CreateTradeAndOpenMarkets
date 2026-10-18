package com.errantmc.markets.item;

import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.data.CoinDenomination;
import com.errantmc.markets.item.wallet.WalletItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import com.errantmc.markets.item.idcard.IdentityCardItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class TradeItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ErrantTrade.MOD_ID);

    public static final DeferredItem<Item> COIN_COPPER_BIT   = ITEMS.register("coin_copper_bit",
            () -> new CoinItem(new Item.Properties(), CoinDenomination.COPPER_BIT));

    public static final DeferredItem<Item> COIN_COPPER_SHARD   = ITEMS.register("coin_copper_shard",
            () -> new CoinItem(new Item.Properties(), CoinDenomination.COPPER_SHARD));

    public static final DeferredItem<Item> COIN_SILVER_BIT = ITEMS.register("coin_silver_bit",
            () -> new CoinItem(new Item.Properties(), CoinDenomination.SILVER_BIT));

    public static final DeferredItem<Item> COIN_SILVER_CROWN = ITEMS.register("coin_silver_crown",
            () -> new CoinItem(new Item.Properties(), CoinDenomination.SILVER_CROWN));

    public static final DeferredItem<Item> COIN_GOLD_MARK = ITEMS.register("coin_gold_mark",
            () -> new CoinItem(new Item.Properties(), CoinDenomination.GOLD_MARK));

    public static final DeferredItem<Item> COIN_GOLD_REGAL = ITEMS.register("coin_gold_regal",
            () -> new CoinItem(new Item.Properties(), CoinDenomination.GOLD_REGAL));

    public static final DeferredItem<Item> COIN_EMERALD_SUN  = ITEMS.register("coin_emerald_sun",
            () -> new CoinItem(new Item.Properties(), CoinDenomination.EMERALD_SUN));

    public static final DeferredItem<Item> COIN_EMERALD_THRONE  = ITEMS.register("coin_emerald_throne",
            () -> new CoinItem(new Item.Properties(), CoinDenomination.EMERALD_THRONE));

    public static final DeferredItem<Item> LEATHER_WALLET  = ITEMS.register("leather_wallet",
            () -> new WalletItem(new Item.Properties()));

    public static final DeferredItem<Item> ID_CARD = ITEMS.register("id_card",
            () -> new IdentityCardItem(new Item.Properties()));

    public static final DeferredItem<Item> COIN_STACK_ITEM  = ITEMS.register("coin_stack",
            () -> new CoinStackItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
