package net.errantchaosie.tradeandopenmarkets.registry;

import com.google.common.collect.ImmutableSet;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportSounds;
import net.errantchaosie.tradeandopenmarkets.TradeAndOpenMarketsMod;
import net.errantchaosie.tradeandopenmarkets.block.SafeBlock;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.concurrent.Immutable;

public class TAOMVillagers {
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(BuiltInRegistries.POINT_OF_INTEREST_TYPE, TradeAndOpenMarketsMod.MOD_ID);
    public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS =
            DeferredRegister.create(BuiltInRegistries.VILLAGER_PROFESSION, TradeAndOpenMarketsMod.MOD_ID);

    public static final Holder<PoiType> BANKER_POI = POI_TYPES.register("banker_poi",
            () -> new PoiType(ImmutableSet.copyOf(TAOMBlocks.SAFE_BLOCK.get().getStateDefinition().getPossibleStates()), 1, 1));

    public static final Holder<VillagerProfession> BANKER_PROFESSION = VILLAGER_PROFESSIONS.register("banker",
            () -> new VillagerProfession("banker", holder -> holder.value() == BANKER_POI.value(),
                    poiTypeHolder -> poiTypeHolder.value() == BANKER_POI.value(), ImmutableSet.of(), ImmutableSet.of(),
                    TAOMSounds.COIN_DROP.get()));

    public static void register(IEventBus eventBus) {
        POI_TYPES.register(eventBus);
        VILLAGER_PROFESSIONS.register(eventBus);
    }
}
