package com.errantmc.markets.npcinteractions.trading;

import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.economy.VillageEconomy;
import com.mojang.serialization.Codec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** Shop state kept on the villager itself, so it is saved with the villager. */
public final class TradeAttachments {
    private TradeAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ErrantTrade.MOD_ID);

    /** Copper bits of trade not yet worth a whole point of experience (see TradeXp). */
    public static final Supplier<AttachmentType<Long>> XP_REMAINDER = ATTACHMENT_TYPES.register("trade_xp_remainder",
            () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build());

    /** Today's remaining stock of the villager's defined shop (see ShopStock). */
    public static final Supplier<AttachmentType<ShopStock>> SHOP_STOCK = ATTACHMENT_TYPES.register("shop_stock",
            () -> AttachmentType.builder(() -> ShopStock.NONE).serialize(ShopStock.CODEC).build());

    /**
     * The villager's till, in copper bits (see VillagerBalance). {@link VillagerBalance#UNSET} until
     * it first trades, when it gets its starting balance.
     */
    public static final Supplier<AttachmentType<Long>> BALANCE = ATTACHMENT_TYPES.register("balance",
            () -> AttachmentType.builder(() -> VillagerBalance.UNSET).serialize(Codec.LONG).build());

    /** The last day the villager's wages and upkeep were settled (see VillageEconomy), or VillageEconomy.UNSET. */
    public static final Supplier<AttachmentType<Long>> SETTLED_DAY = ATTACHMENT_TYPES.register("settled_day",
            () -> AttachmentType.builder(() -> VillageEconomy.UNSET).serialize(Codec.LONG).build());

    /** What restocking the goods the villager has sold costs, paid at its next settlement. */
    public static final Supplier<AttachmentType<Long>> RESTOCK_OWED = ATTACHMENT_TYPES.register("restock_owed",
            () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build());

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
