package com.errantmc.markets.block.entity;

import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.block.TradeBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class TradeBlockEntities {

    // Create a DR for the BlockEntityType registry
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ErrantTrade.MOD_ID);

    // Register your BE type (note the 2-arg factory expected by vanilla)
    public static final Supplier<BlockEntityType<SafeBlockEntity>> SAFE =
            BLOCK_ENTITIES.register("safe", () ->
                    BlockEntityType.Builder.of(
                            // uses the 2-arg ctor shown below
                            SafeBlockEntity::new,
                            // works whether SAFE is a BlockEntry or a RegistryObject
                            TradeBlocks.SAFE.get()
                    ).build(null)
            );

    // Register your BE type (note the 2-arg factory expected by vanilla)
    public static final Supplier<BlockEntityType<CoinBlockEntity>> COIN_PILE =
            BLOCK_ENTITIES.register("coin_pile", () ->
                    BlockEntityType.Builder.of(
                            // uses the 2-arg ctor shown below
                            CoinBlockEntity::new,
                            // works whether SAFE is a BlockEntry or a RegistryObject
                            TradeBlocks.COIN_PILE.get()
                    ).build(null)
            );

    // Call this from your @Mod constructor
    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }

    private TradeBlockEntities() {}

}
