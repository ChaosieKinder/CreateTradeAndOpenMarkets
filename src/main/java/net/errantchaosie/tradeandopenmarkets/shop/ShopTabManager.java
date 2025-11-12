package net.errantchaosie.tradeandopenmarkets.shop;

import net.errantchaosie.tradeandopenmarkets.TradeAndOpenMarketsMod;
import net.errantchaosie.tradeandopenmarkets.shop.tab.*;
import net.errantchaosie.tradeandopenmarkets.shop.tab.config.ShopTabDefinition;
import net.errantchaosie.tradeandopenmarkets.shop.tab.types.BuyShopTabType;
import net.errantchaosie.tradeandopenmarkets.shop.tab.types.SellShopTabType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the shop tabs that should be shown for the current merchant.
 */
public final class ShopTabManager {
    public static final ShopTabManager INSTANCE = new ShopTabManager();

    private final Map<CacheKey, List<ShopTab>> cachedTabs = new HashMap<>();
    private long cachedDayIndex = Long.MIN_VALUE;

    private ShopTabManager() {
        ShopTabRegistry.register(new BuyShopTabType());
        ShopTabRegistry.register(new SellShopTabType());
    }

    public synchronized List<ShopTab> resolveTabs(@Nullable Merchant merchant, MerchantOffers offers, Level level) {
        long dayIndex = level.getDayTime() / 24000L;
        long seed = level.getSeed() ^ dayIndex;
        if (merchant != null) {
            seed ^= merchant.getId();
        }
        RandomSource random = RandomSource.create(seed);
        ShopTabGenerationContext context = new ShopTabGenerationContext(merchant, offers, level, dayIndex, random);

        if (dayIndex != this.cachedDayIndex) {
            this.cachedDayIndex = dayIndex;
            this.cachedTabs.clear();
        }

        List<ShopTab> result = new ArrayList<>();
        Optional<ResourceLocation> professionId = resolveProfessionId(merchant);
        CacheKey cacheKey = cacheKeyFor(merchant, professionId);
        if (cacheKey != null) {
            List<ShopTab> cached = this.cachedTabs.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }

        boolean usedDefinition = false;
        if (professionId.isPresent()) {
            ShopDefinitionLoader.getInstance().getDefinition(professionId.get())
                    .ifPresent(definition -> definition.tabs().forEach(tabDef -> {
                        ShopTabType<?> type = ShopTabRegistry.get(tabDef.type());
                        if (type == null) {
                            TradeAndOpenMarketsMod.LOGGER.warn("Unknown shop tab type {} referenced in definition for {}", tabDef.type(), professionId.get());
                            return;
                        }
                        result.add(type.create(tabDef, context));
                    }));
            usedDefinition = !result.isEmpty();
        }

        if (!result.isEmpty()) {
            applyAutoBuybacks(result);
            List<ShopTab> immutable = List.copyOf(result);
            if (usedDefinition && cacheKey != null) {
                this.cachedTabs.put(cacheKey, immutable);
            }
            return immutable;
        }

        // fallback to default buy/sell using merchant offers
        List<ShopTab> fallback = fallbackFromOffers(offers);
        applyAutoBuybacks(fallback);
        return fallback.isEmpty() ? List.of() : List.copyOf(fallback);
    }

    public synchronized void invalidate() {
        this.cachedTabs.clear();
        this.cachedDayIndex = Long.MIN_VALUE;
    }

    private List<ShopTab> fallbackFromOffers(MerchantOffers offers) {
        List<ShopEntry> buyEntries = new ArrayList<>();
        List<ShopEntry> sellEntries = new ArrayList<>();
        for (MerchantOffer offer : offers) {
            if (offer.getResult().is(Items.EMERALD)) {
                // Villager buys items from the player -> sell tab entry
                MerchantOfferSellAdapter.addSellEntry(offer, sellEntries);
            } else {
                MerchantOfferBuyAdapter.addBuyEntry(offer, buyEntries);
            }
        }
        List<ShopTab> tabs = new ArrayList<>();
        if (!buyEntries.isEmpty()) {
            tabs.add(new BuyShopTab(BuyShopTabType.ID, ComponentHelper.buyTitle(), buyEntries));
        }
        if (!sellEntries.isEmpty()) {
            tabs.add(new SellShopTab(SellShopTabType.ID, ComponentHelper.sellTitle(), sellEntries, false, 0.5D));
        }
        return tabs;
    }

    private void applyAutoBuybacks(List<ShopTab> tabs) {
        if (tabs.isEmpty()) {
            return;
        }
        List<BuyShopTab> buyTabs = new ArrayList<>();
        List<SellShopTab> sellTabs = new ArrayList<>();
        for (ShopTab tab : tabs) {
            if (tab instanceof BuyShopTab buyTab) {
                buyTabs.add(buyTab);
            } else if (tab instanceof SellShopTab sellTab) {
                sellTabs.add(sellTab);
            }
        }
        if (buyTabs.isEmpty() || sellTabs.isEmpty()) {
            return;
        }
        for (SellShopTab sellTab : sellTabs) {
            if (!sellTab.autoBuybacksEnabled()) {
                continue;
            }
            for (BuyShopTab buyTab : buyTabs) {
                sellTab.addBuybacksFrom(buyTab);
            }
        }
    }

    private Optional<ResourceLocation> resolveProfessionId(@Nullable Merchant merchant) {
        if (merchant instanceof Villager villager) {
            VillagerProfession profession = villager.getVillagerData().getProfession();
            return Optional.ofNullable(BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession));
        }
        return Optional.empty();
    }

    private CacheKey cacheKeyFor(@Nullable Merchant merchant, Optional<ResourceLocation> professionId) {
        if (merchant == null || professionId.isEmpty()) {
            return null;
        }
        if (merchant instanceof net.minecraft.world.entity.Entity entity) {
            UUID uuid = entity.getUUID();
            return new CacheKey(professionId.get(), uuid);
        }
        return null;
    }

    private static class ComponentHelper {
        private static final Component BUY_TITLE = Component.literal("Buy");
        private static final Component SELL_TITLE = Component.literal("Sell");

        static Component buyTitle() {
            return BUY_TITLE;
        }

        static Component sellTitle() {
            return SELL_TITLE;
        }
    }

    private static class MerchantOfferBuyAdapter {
        private static void addBuyEntry(MerchantOffer offer, List<ShopEntry> entries) {
            int price = offer.getCostA().getCount();
            if (!offer.getCostB().isEmpty()) {
                price += offer.getCostB().getCount();
            }
            entries.add(new ShopEntry(offer.getResult().copy(), price, offer.getMaxUses()));
        }
    }

    private static class MerchantOfferSellAdapter {
        private static void addSellEntry(MerchantOffer offer, List<ShopEntry> entries) {
            int reward = offer.getResult().getCount();
            entries.add(new ShopEntry(offer.getCostA().copy(), reward, offer.getMaxUses()));
        }
    }

    private record CacheKey(ResourceLocation profession, UUID traderId) { }
}
