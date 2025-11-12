package net.errantchaosie.tradeandopenmarkets.shop.tab.types;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.errantchaosie.tradeandopenmarkets.TradeAndOpenMarketsMod;
import net.errantchaosie.tradeandopenmarkets.shop.tab.*;
import net.errantchaosie.tradeandopenmarkets.shop.tab.config.ShopTabDefinition;
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

public class BuyShopTabType implements ShopTabType {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(TradeAndOpenMarketsMod.MOD_ID, "buy");
    private static final Logger LOGGER = TradeAndOpenMarketsMod.LOGGER;

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public ShopTab create(ShopTabDefinition definition, ShopTabGenerationContext context) {
        List<ShopEntry> entries = readEntries(definition, context.random());
        List<ShopEntry> filtered = applyLevelLimit(entries, context.merchant());
        return new BuyShopTab(id(), definition.title(), filtered);
    }

    private List<ShopEntry> readEntries(ShopTabDefinition definition, RandomSource random) {
        JsonObject data = definition.data();
        JsonArray items = data.has("entries") ? data.getAsJsonArray("entries") : new JsonArray();
        List<ShopEntry> results = new ArrayList<>();
        for (JsonElement element : items) {
            if (!element.isJsonObject()) {
                LOGGER.warn("Invalid entry in buy tab '{}': expected object but got {}", definition.type(), element);
                continue;
            }
            JsonObject entry = element.getAsJsonObject();
            JsonElement itemElement = entry.get("item");
            if (itemElement == null) {
                LOGGER.warn("Missing 'item' field for buy tab entry in {}", definition.type());
                continue;
            }
            Optional<ItemStack> stackOpt = ItemStack.CODEC.parse(JsonOps.INSTANCE, itemElement)
                    .resultOrPartial(message -> LOGGER.warn("Failed to parse buy tab item: {}", message));
            if (stackOpt.isEmpty()) {
                continue;
            }
            ItemStack stack = stackOpt.get();
            int price = entry.has("price") ? entry.get("price").getAsInt() : 0;
            int stock = resolveStock(definition.type(), entry, random);
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
                LOGGER.warn("Ignoring non-object stock option in tab '{}': {}", tabId, optionElement);
                continue;
            }
            JsonObject option = optionElement.getAsJsonObject();
            if (!option.has("value")) {
                LOGGER.warn("Ignoring stock option missing 'value' in tab '{}': {}", tabId, option);
                continue;
            }
            int value = option.get("value").getAsInt();
            int weight = option.has("weight") ? Math.max(0, option.get("weight").getAsInt()) : 1;
            if (weight <= 0) {
                LOGGER.warn("Ignoring stock option with non-positive weight in tab '{}': {}", tabId, option);
                continue;
            }
            int plusOrMinus = option.has("plus_or_minus") ? Math.max(0, option.get("plus_or_minus").getAsInt()) : 0;
            parsedOptions.add(new WeightedStockOption(value, weight, plusOrMinus));
            totalWeight += weight;
        }
        if (parsedOptions.isEmpty() || totalWeight <= 0) {
            LOGGER.warn("No valid stock options found for tab '{}'", tabId);
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
