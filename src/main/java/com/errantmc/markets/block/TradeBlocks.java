package com.errantmc.markets.block;

import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.item.TradeItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class TradeBlocks {

    // DeferredRegister for blocks
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(ErrantTrade.MOD_ID);

    // safe_block registration
    public static final Supplier<SafeBlock> SAFE_BLOCK =
            registerBlockItem("safe_block", () ->
                    new SafeBlock(BlockBehaviour.Properties.of()
                            .strength(1200.0F)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
                            .sound(SoundType.METAL)
                    )
            );

    // coin_block registration
    public static final Supplier<CoinBlock> COIN_BLOCK =
            registerBlock("coin_block", () ->
                    new CoinBlock(BlockBehaviour.Properties.of())
            );


    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        return BLOCKS.register(name, block);
    }

    private static <T extends Block> DeferredBlock<T> registerBlockItem(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        TradeItems.ITEMS.register(name, () -> new BlockItem(toReturn.get(), new Item.Properties()));
        return toReturn;
    }
    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }

    private TradeBlocks() {}
}
