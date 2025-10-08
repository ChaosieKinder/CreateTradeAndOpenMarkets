package net.errantchaosie.tradeandopenmarkets.block.entity;

import net.errantchaosie.tradeandopenmarkets.TradeAndOpenMarketsMod;
import net.errantchaosie.tradeandopenmarkets.block.TAOMBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class TAOMBlockEntities {

    // Create a DR for the BlockEntityType registry
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TradeAndOpenMarketsMod.MOD_ID);

    // Register your BE type (note the 2-arg factory expected by vanilla)
    public static final Supplier<BlockEntityType<SafeBlockEntity>> SAFE_BLOCKENTITY =
            BLOCK_ENTITIES.register("safe_blockentity", () ->
                    BlockEntityType.Builder.of(
                            // uses the 2-arg ctor shown below
                            SafeBlockEntity::new,
                            // works whether SAFE_BLOCK is a BlockEntry or a RegistryObject
                            TAOMBlocks.SAFE_BLOCK.get()
                    ).build(null)
            );

    // Register your BE type (note the 2-arg factory expected by vanilla)
    public static final Supplier<BlockEntityType<CoinBlockEntity>> COIN_BLOCKENTITY =
            BLOCK_ENTITIES.register("coin_blockentity", () ->
                    BlockEntityType.Builder.of(
                            // uses the 2-arg ctor shown below
                            CoinBlockEntity::new,
                            // works whether SAFE_BLOCK is a BlockEntry or a RegistryObject
                            TAOMBlocks.COIN_BLOCK.get()
                    ).build(null)
            );

    // Call this from your @Mod constructor
    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }

    private TAOMBlockEntities() {}

}
