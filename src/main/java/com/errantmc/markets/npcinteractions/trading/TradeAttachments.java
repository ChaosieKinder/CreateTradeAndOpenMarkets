package com.errantmc.markets.npcinteractions.trading;

import com.errantmc.markets.ErrantTrade;
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

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
