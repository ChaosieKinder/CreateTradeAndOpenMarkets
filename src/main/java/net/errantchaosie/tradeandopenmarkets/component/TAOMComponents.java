package net.errantchaosie.tradeandopenmarkets.component;

import net.errantchaosie.tradeandopenmarkets.TradeAndOpenMarketsMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class TAOMComponents
{
    public static final DeferredRegister.DataComponents DATA_COMPONENT_TYPES =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, TradeAndOpenMarketsMod.MOD_ID);



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
