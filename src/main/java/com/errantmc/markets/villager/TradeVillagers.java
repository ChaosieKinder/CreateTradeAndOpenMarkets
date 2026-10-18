package com.errantmc.markets.villager;

import com.google.common.collect.ImmutableSet;
import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.block.TradeBlocks;
import com.errantmc.markets.registry.TradeSounds;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class TradeVillagers {
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(BuiltInRegistries.POINT_OF_INTEREST_TYPE, ErrantTrade.MOD_ID);
    public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS =
            DeferredRegister.create(BuiltInRegistries.VILLAGER_PROFESSION, ErrantTrade.MOD_ID);

    public static final Holder<PoiType> BANKER_POI = POI_TYPES.register("banker",
            () -> new PoiType(ImmutableSet.copyOf(TradeBlocks.SAFE.get().getStateDefinition().getPossibleStates()), 1, 1));

    public static final Holder<VillagerProfession> BANKER_PROFESSION = VILLAGER_PROFESSIONS.register("banker",
            () -> new VillagerProfession("banker", holder -> holder.value() == BANKER_POI.value(),
                    poiTypeHolder -> poiTypeHolder.value() == BANKER_POI.value(), ImmutableSet.of(), ImmutableSet.of(),
                    TradeSounds.COIN_DROP.get()));

    public static void register(IEventBus eventBus) {
        POI_TYPES.register(eventBus);
        VILLAGER_PROFESSIONS.register(eventBus);
    }
}
