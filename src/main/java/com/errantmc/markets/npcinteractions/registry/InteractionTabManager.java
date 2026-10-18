package com.errantmc.markets.npcinteractions.registry;

import com.mojang.logging.LogUtils;
import com.errantmc.markets.Config;
import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.npcinteractions.components.InteractionTab;
import com.errantmc.markets.npcinteractions.tabs.buy.BuyShopTab;
import com.errantmc.markets.npcinteractions.tabs.buy.BuyShopTabType;
import com.errantmc.markets.npcinteractions.tabs.sell.SellShopTab;
import com.errantmc.markets.npcinteractions.tabs.sell.SellShopTabType;
import com.errantmc.markets.npcinteractions.trading.ShopEntry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves the npcinteractions tabs that should be shown for the current villager.
 */
public final class InteractionTabManager {
    public static final Logger LOG = LogUtils.getLogger();
    public static final InteractionTabManager INSTANCE = new InteractionTabManager();

    private final Map<InteractionTabGenerationContext.CacheKey, List<InteractionTab>> cachedTabs = new HashMap<>();
    private long cachedDayIndex = Long.MIN_VALUE;

    private InteractionTabManager() {
        InteractionTabRegistry.register(new BuyShopTabType());
        InteractionTabRegistry.register(new SellShopTabType());
    }

    public void writeToStream(RegistryFriendlyByteBuf buffer, List<InteractionTab> tabs) {
        buffer.writeVarInt(tabs.size());
        for (InteractionTab tab : tabs) {
            buffer.writeResourceLocation(tab.type());
            tab.writeToStream(buffer);
        }
    }

    public List<InteractionTab> readFromStream(RegistryFriendlyByteBuf buffer) {
        List<InteractionTab> tabs = new ArrayList<>();
        int tabCount = buffer.readVarInt();
        for (int i = 0; i < tabCount; i++) {
            var typeId = buffer.readResourceLocation();
            var type = InteractionTabRegistry.get(typeId);
            if (type == null) {
                ErrantTrade.LOGGER.warn("Unknown npcinteractions tabs type {} received from server", typeId);
                continue;
            }

            var tab = type.readFromBuffer(buffer);
            if (tab != null) {
                tabs.add(tab);
            }
        }
        return tabs;
    }

    public synchronized List<InteractionTab> resolveTabs(@Nullable Merchant merchant, MerchantOffers offers, Level level) {
        InteractionTabGenerationContext context = new InteractionTabGenerationContext(merchant, offers, level);
        LOG.debug("resolveTabs context: {}", context.toString());
        LOG.debug("resolveTabs context.merchant: {}", context.getMerchant());


        if (context.getDayIndex() != this.cachedDayIndex) {
            this.cachedDayIndex = context.getDayIndex();
            this.cachedTabs.clear();
        }

        List<InteractionTab> result = new ArrayList<>();
        var cacheKey = context.getCacheKey();
        if (cacheKey.isPresent()) {
            List<InteractionTab> cached = this.cachedTabs.get(cacheKey.get());
            if (cached != null) {
                return cached;
            }
        }

        InteractionDefinitionLoader.getInstance().getAllDefinitions().forEach((tab) -> {
            LOG.debug("resolveTabs available definition: {}", tab.profession());
        });

        boolean usedDefinition = false;
        if (context.getProfessionId().isPresent()) {
            LOG.debug("resolveTabs using profession id {}", context.getProfessionId().get());
            InteractionDefinitionLoader.getInstance().getDefinition(context.getProfessionId().get())
                .ifPresent(definition -> definition.tabs().forEach(tabDef -> {
                    InteractionTabType type = InteractionTabRegistry.get(tabDef.getType());
                    LOG.debug("resolveTabs using type {}", type);
                    if (type == null) {
                        ErrantTrade.LOGGER.warn("Unknown npcinteractions tabs type {} referenced in definition for {}",
                                tabDef.getType(), context.getProfessionId().get());
                        return;
                    }
                    LOG.debug("Creating tab of type {} for profession {}",
                            tabDef.getType(), context.getProfessionId().get());
                    result.add(type.create(tabDef, context));
                }));
            usedDefinition = !result.isEmpty();
        }

        if (!result.isEmpty()) {
            applyAutoBuybacks(result);
            List<InteractionTab> immutable = List.copyOf(result);
            if (usedDefinition && cacheKey != null) {
                this.cachedTabs.put(cacheKey.get(), immutable);
            }
            return immutable;
        }

        LOG.debug("No tabs found from definition, falling back to offers for merchant {}", merchant);
        // fallback to default buy/sell using merchant offers
        List<InteractionTab> fallback = fallbackFromOffers(offers);
        applyAutoBuybacks(fallback);
        return fallback.isEmpty() ? List.of() : List.copyOf(fallback);
    }

    public synchronized void invalidate() {
        this.cachedTabs.clear();
        this.cachedDayIndex = Long.MIN_VALUE;
    }

    /**
     * A villager with no shop definition sells and buys what its vanilla offers say, at
     * {@code trading.emeraldValue} copper bits per emerald (100 by default; author's decision,
     * 18 Oct). Each entry stays linked to its offer: stock is the offer's remaining uses, a trade
     * uses it up, and the villager restocks at its workstation as in vanilla. Prices are the
     * offer's current ones, after demand and discounts. A cost that is not emeralds, such as the
     * librarian's book, becomes an item the player must also hand over.
     */
    private List<InteractionTab> fallbackFromOffers(MerchantOffers offers) {
        int emerald = Config.EMERALD_VALUE.get();
        List<ShopEntry> buyEntries = new ArrayList<>();
        List<ShopEntry> sellEntries = new ArrayList<>();
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer offer = offers.get(i);
            int remaining = Math.max(0, offer.getMaxUses() - offer.getUses());
            List<ItemStack> goods = new ArrayList<>(2);
            int emeralds = 0;
            for (ItemStack cost : List.of(offer.getCostA(), offer.getCostB())) {
                if (cost.isEmpty()) continue;
                if (cost.is(Items.EMERALD)) emeralds += cost.getCount();
                else goods.add(cost);
            }
            if (offer.getResult().is(Items.EMERALD)) {
                // The villager buys goods for emeralds: the player sells
                if (goods.isEmpty()) continue;
                ItemStack extra = goods.size() > 1 ? goods.get(1) : ItemStack.EMPTY;
                sellEntries.add(new ShopEntry(goods.get(0), offer.getResult().getCount() * emerald, remaining, extra, i));
            } else {
                // The villager sells: the player pays the emeralds as coins, plus any goods
                if (goods.size() > 1) {
                    LOG.debug("Skipping offer {} with two non-emerald costs", offer);
                    continue;
                }
                ItemStack extra = goods.isEmpty() ? ItemStack.EMPTY : goods.get(0);
                buyEntries.add(new ShopEntry(offer.getResult(), emeralds * emerald, remaining, extra, i));
            }
        }
        List<InteractionTab> tabs = new ArrayList<>();
        if (!buyEntries.isEmpty()) {
            tabs.add(new BuyShopTab(BuyShopTabType.ID, Component.translatable("shop.erranttrade.buy"), buyEntries));
        }
        if (!sellEntries.isEmpty()) {
            tabs.add(new SellShopTab(SellShopTabType.ID, Component.translatable("shop.erranttrade.sell"), sellEntries, false, 0.5D));
        }
        return tabs;
    }

    private void applyAutoBuybacks(List<InteractionTab> tabs) {
        if (tabs.isEmpty()) {
            return;
        }
        List<BuyShopTab> buyTabs = new ArrayList<>();
        List<SellShopTab> sellTabs = new ArrayList<>();
        for (InteractionTab tab : tabs) {
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




}
