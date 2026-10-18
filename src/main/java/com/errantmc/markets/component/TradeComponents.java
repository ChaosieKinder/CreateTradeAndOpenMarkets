package com.errantmc.markets.component;

import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.domain.Purse;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class TradeComponents
{
    public static final DeferredRegister.DataComponents DATA_COMPONENT_TYPES =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ErrantTrade.MOD_ID);

    /** The coins on a wallet or a coin stack. The id predates the Purse type and is a save format. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Purse>> PURSE =
            DATA_COMPONENT_TYPES.register("currency_data", () ->
                    DataComponentType.<Purse>builder()
                            .persistent(Purses.CODEC)
                            .networkSynchronized(Purses.STREAM_CODEC)
                            .build()
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<IdentityCardData>> IDENTITY_CARD_DATA =
            DATA_COMPONENT_TYPES.register("identity_card_data", () ->
                    DataComponentType.<IdentityCardData>builder()
                            .persistent(IdentityCardData.CODEC)
                            .networkSynchronized(IdentityCardData.STREAM_CODEC)
                            .build()
            );
    
    // NEW: return Supplier<DataComponentType<T>> and use registerComponentType(...)
    public static <T> Supplier<DataComponentType<T>> register(
            String name,
            UnaryOperator<DataComponentType.Builder<T>> builderOperator
    ) {
        return DATA_COMPONENT_TYPES.registerComponentType(
                name,
                builderOperator
        );
    }

    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}
