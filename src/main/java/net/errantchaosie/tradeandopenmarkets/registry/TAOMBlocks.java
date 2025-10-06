package net.errantchaosie.tradeandopenmarkets.registry;

import com.tterrag.registrate.util.entry.BlockEntry;
import net.errantchaosie.tradeandopenmarkets.TradeAndOpenMarketsMod;
import net.errantchaosie.tradeandopenmarkets.block.SafeBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class TAOMBlocks {

    // DeferredRegister for blocks
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, TradeAndOpenMarketsMod.MOD_ID);

    // safe_block registration
    public static final Supplier<SafeBlock> SAFE_BLOCK =
            BLOCKS.register("safe_block", () ->
                    new SafeBlock(BlockBehaviour.Properties.of()
                            .strength(8.0F)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
                            .sound(SoundType.METAL)
                    )
            );

    // Call this from your @Mod constructor
    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }

    private TAOMBlocks() {}
}
