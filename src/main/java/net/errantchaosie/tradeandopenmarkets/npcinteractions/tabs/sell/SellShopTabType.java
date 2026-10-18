package net.errantchaosie.tradeandopenmarkets.npcinteractions.tabs.sell;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.errantchaosie.tradeandopenmarkets.TradeAndOpenMarketsMod;
import net.errantchaosie.tradeandopenmarkets.npcinteractions.components.InteractionTab;
import net.errantchaosie.tradeandopenmarkets.npcinteractions.registry.InteractionTabType;
import net.errantchaosie.tradeandopenmarkets.npcinteractions.trading.ShopEntry;
import net.errantchaosie.tradeandopenmarkets.npcinteractions.registry.InteractionTabConfig;
import net.errantchaosie.tradeandopenmarkets.npcinteractions.registry.InteractionTabGenerationContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SellShopTabType implements InteractionTabType {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(TradeAndOpenMarketsMod.MOD_ID, "sell");
    private static final Logger LOGGER = TradeAndOpenMarketsMod.LOGGER;

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public InteractionTab create(InteractionTabConfig definition, InteractionTabGenerationContext context) {
        List<ShopEntry> entries = readEntries(definition, context.getRandom());
        AutoBuybackSettings settings = parseBuybackSettings(definition);
        return new SellShopTab(id(), definition.getTitle(), entries, settings.enabled(), settings.percentage());
    }

    @Override
    public InteractionTab readFromBuffer(RegistryFriendlyByteBuf buffer) {
        return new SellShopTab(id(), buffer);
    }

    private List<ShopEntry> readEntries(InteractionTabConfig definition, RandomSource random) {
        JsonObject data = definition.getRawData();
        JsonArray items = data.has("entries") ? data.getAsJsonArray("entries") : new JsonArray();
        List<ShopEntry> results = new ArrayList<>();
        for (JsonElement element : items) {
            if (!element.isJsonObject()) {
                LOGGER.warn("Invalid entry in sell tabs '{}': expected object but got {}", definition.getType(), element);
                continue;
            }
            JsonObject entry = element.getAsJsonObject();
            JsonElement itemElement = entry.get("item");
            if (itemElement == null) {
                LOGGER.warn("Missing 'item' field for sell tabs entry in {}", definition.getType());
                continue;
            }
            Optional<ItemStack> stackOpt = ItemStack.CODEC.parse(JsonOps.INSTANCE, itemElement)
                    .resultOrPartial(message -> LOGGER.warn("Failed to parse sell tabs item: {}", message));
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

    private AutoBuybackSettings parseBuybackSettings(InteractionTabConfig definition) {
        JsonObject data = definition.getRawData();
        if (!data.has("auto_buybacks")) {
            return AutoBuybackSettings.DISABLED;
        }
        JsonObject buyback = data.getAsJsonObject("auto_buybacks");
        boolean enabled = buyback.has("enabled") && buyback.get("enabled").getAsBoolean();
        if (!enabled) {
            return AutoBuybackSettings.DISABLED;
        }
        double percentage = buyback.has("percentage") ? buyback.get("percentage").getAsDouble() : 0.5D;
        if (percentage > 1.0D) {
            percentage = percentage / 100.0D;
        }
        percentage = Math.max(0.0D, percentage);
        return new AutoBuybackSettings(true, percentage);
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

    private record AutoBuybackSettings(boolean enabled, double percentage) {
        private static final AutoBuybackSettings DISABLED = new AutoBuybackSettings(false, 0.5D);
    }
}
