package com.errantmc.markets.npcinteractions.tabs.buy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.npcinteractions.components.InteractionTab;
import com.errantmc.markets.npcinteractions.registry.InteractionTabType;
import com.errantmc.markets.npcinteractions.trading.ShopEntry;
import com.errantmc.markets.npcinteractions.registry.InteractionTabConfig;
import com.errantmc.markets.npcinteractions.registry.InteractionTabGenerationContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.entity.npc.Villager;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

public class BuyShopTabType implements InteractionTabType {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ErrantTrade.MOD_ID, "buy");
    private static final Logger LOGGER = ErrantTrade.LOGGER;

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public InteractionTab create(InteractionTabConfig definition, InteractionTabGenerationContext context) {
        List<ShopEntry> entries = readEntries(definition, context.getRandom());
        List<ShopEntry> filtered = applyLevelLimit(entries, context.getMerchant());
        return new BuyShopTab(id(), definition.getTitle(), filtered);
    }

    @Override
    public InteractionTab readFromBuffer(RegistryFriendlyByteBuf buffer) {
        return new BuyShopTab(id(), buffer);
    }

    private List<ShopEntry> readEntries(InteractionTabConfig definition, RandomSource random) {
        JsonObject data = definition.getRawData();
        JsonArray items = data.has("entries") ? data.getAsJsonArray("entries") : new JsonArray();
        List<ShopEntry> results = new ArrayList<>();
        for (JsonElement element : items) {
            if (!element.isJsonObject()) {
                LOGGER.warn("Invalid entry in buy tabs '{}': expected object but got {}", definition.getType(), element);
                continue;
            }
            JsonObject entry = element.getAsJsonObject();
            JsonElement itemElement = entry.get("item");
            if (itemElement == null) {
                LOGGER.warn("Missing 'item' field for buy tabs entry in {}", definition.getType());
                continue;
            }
            Optional<ItemStack> stackOpt = ItemStack.CODEC.parse(JsonOps.INSTANCE, itemElement)
                    .resultOrPartial(message -> LOGGER.warn("Failed to parse buy tabs item: {}", message));
            if (stackOpt.isEmpty()) {
                continue;
            }
            ItemStack stack = stackOpt.get();
            int price = entry.has("price") ? entry.get("price").getAsInt() : 0;
            int stock = resolveStock(definition.getType(), entry, random);
            results.add(new ShopEntry(stack, price, stock));
        }
        return results;
    }

    private List<ShopEntry> applyLevelLimit(List<ShopEntry> entries, @Nullable Merchant merchant) {
        if (entries.isEmpty() || merchant == null) {
            return entries;
        }
        if (!(merchant instanceof Villager villager)) {
            return entries;
        }
        int level = villager.getVillagerData().getLevel();
        int limit = Mth.clamp(4 + Math.max(level - 1, 0) * 2, 0, 12);
        if (limit <= 0 || entries.size() <= limit) {
            return entries;
        }
        return List.copyOf(entries.subList(0, limit));
    }

    private int resolveStock(ResourceLocation tabId, JsonObject entry, RandomSource random) {
        if (!entry.has("stock")) {
            return ShopEntry.UNLIMITED_STOCK;
        }
        JsonElement stockElement = entry.get("stock");
        if (stockElement.isJsonPrimitive()) {
            return stockElement.getAsInt();
        }
        if (stockElement.isJsonObject()) {
            JsonObject stockObject = stockElement.getAsJsonObject();
            if (stockObject.has("options")) {
                return resolveWeightedStockOptions(tabId, stockObject.getAsJsonArray("options"), random);
            }
        }
        Optional<IntProvider> provider = IntProvider.CODEC.parse(JsonOps.INSTANCE, stockElement)
                .resultOrPartial(message -> LOGGER.warn("Failed to parse stock provider: {}", message));
        IntProvider intProvider = provider.orElse(ConstantInt.of(ShopEntry.UNLIMITED_STOCK));
        int value = intProvider.sample(random);
        return value <= 0 ? ShopEntry.UNLIMITED_STOCK : value;
    }

    private int resolveWeightedStockOptions(ResourceLocation tabId, JsonArray options, RandomSource random) {
        List<WeightedStockOption> parsedOptions = new ArrayList<>();
        int totalWeight = 0;
        for (JsonElement optionElement : options) {
            if (!optionElement.isJsonObject()) {
                LOGGER.warn("Ignoring non-object stock option in tabs '{}': {}", tabId, optionElement);
                continue;
            }
            JsonObject option = optionElement.getAsJsonObject();
            if (!option.has("value")) {
                LOGGER.warn("Ignoring stock option missing 'value' in tabs '{}': {}", tabId, option);
                continue;
            }
            int value = option.get("value").getAsInt();
            int weight = option.has("weight") ? Math.max(0, option.get("weight").getAsInt()) : 1;
            if (weight <= 0) {
                LOGGER.warn("Ignoring stock option with non-positive weight in tabs '{}': {}", tabId, option);
                continue;
            }
            int plusOrMinus = option.has("plus_or_minus") ? Math.max(0, option.get("plus_or_minus").getAsInt()) : 0;
            parsedOptions.add(new WeightedStockOption(value, weight, plusOrMinus));
            totalWeight += weight;
        }
        if (parsedOptions.isEmpty() || totalWeight <= 0) {
            LOGGER.warn("No valid stock options found for tabs '{}'", tabId);
            return ShopEntry.UNLIMITED_STOCK;
        }
        int weightSelection = random.nextInt(totalWeight);
        WeightedStockOption selected = null;
        for (WeightedStockOption option : parsedOptions) {
            if (weightSelection < option.weight()) {
                selected = option;
                break;
            }
            weightSelection -= option.weight();
        }
        if (selected == null) {
            // Fallback safety, should never happen but protects against logic errors.
            selected = parsedOptions.get(parsedOptions.size() - 1);
        }
        int value = selected.value();
        int plusOrMinus = selected.plusOrMinus();
        if (plusOrMinus > 0) {
            int delta = random.nextInt(plusOrMinus * 2 + 1) - plusOrMinus;
            value += delta;
        }
        return value <= 0 ? ShopEntry.UNLIMITED_STOCK : value;
    }

    private record WeightedStockOption(int value, int weight, int plusOrMinus) {}
}
