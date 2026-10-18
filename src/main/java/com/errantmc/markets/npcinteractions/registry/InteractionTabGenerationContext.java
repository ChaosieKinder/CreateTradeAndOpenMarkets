package com.errantmc.markets.npcinteractions.registry;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.ClientSideMerchant;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

/**
 * Provides contextual information for npcinteractions tabs generation.
 */
public class InteractionTabGenerationContext {
    public static final Logger LOG = LogUtils.getLogger();

    private final long dayIndex;
    private final RandomSource random;
    @Nullable
    private final Merchant merchant;
    private final MerchantOffers offers;
    private final Level level;

    @Nullable
    private final ResourceLocation professionId;

    @Nullable
    private final CacheKey cacheKey;

     /* Constructs a new generation context for the given merchant and level.
     *
     * @param merchant The merchant, may be null.
     * @param offers   The current offers of the merchant.
     * @param level    The level where the merchant is located.
     */
    public InteractionTabGenerationContext(@Nullable Merchant merchant,
                                           MerchantOffers offers,
                                           Level level) {
        this.merchant = merchant;
        this.offers = offers;
        this.level = level;
        this.dayIndex = level.getDayTime() / 24000L;

        long seed = dayIndex;
        if (level instanceof ServerLevel serverLevel) {
            seed ^= serverLevel.getSeed();
        } else {
            seed ^= level.dimension().location().hashCode();
        }
        if (merchant instanceof Entity entity) {
            UUID uuid = entity.getUUID();
            seed ^= uuid.getMostSignificantBits();
            seed ^= uuid.getLeastSignificantBits();
        }
        this.random = RandomSource.create(seed);

        var vClass = merchant.getClass();
        LOG.info("InteractionTabGenerationContext - merchant class: {}", vClass.getName());
        if (merchant instanceof Villager villager) {
            LOG.info("InteractionTabGenerationContext - merchant is a Villager");
            VillagerProfession profession = villager.getVillagerData().getProfession();
            LOG.info("InteractionTabGenerationContext - villager profession: {}", profession);
            this.professionId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
            UUID uuid = villager.getUUID();
            this.cacheKey = new CacheKey(professionId, uuid);

        } else {
            LOG.info("InteractionTabGenerationContext - merchant is not a Villager");
            this.professionId = null;
            this.cacheKey = null;
        }

    }

    public long getDayIndex() {
        return dayIndex;
    }
    public RandomSource getRandom() {
        return random;
    }
    @Nullable
    public Merchant getMerchant() {
        return merchant;
    }
    public MerchantOffers getOffers() {
        return offers;
    }
    public Level getLevel() {
        return level;
    }

    public Optional<ResourceLocation> getProfessionId() {
        return Optional.ofNullable(professionId);
    }

    public Optional<CacheKey> getCacheKey() {
        return Optional.ofNullable(cacheKey);
    }

    public boolean hasCacheKey() {
        return cacheKey != null;
    }

    @Override
    public String toString() {
        return "ShopTabGenerationContext{" +
                "dayIndex=" + dayIndex +
                ", random=" + random +
                ", merchant=" + merchant +
                ", offers=" + offers +
                ", level=" + level +
                ", professionId=" + professionId +
                ", cacheKey=" + cacheKey +
                '}';
    }

    public record CacheKey(ResourceLocation profession, UUID traderId) { }

}
